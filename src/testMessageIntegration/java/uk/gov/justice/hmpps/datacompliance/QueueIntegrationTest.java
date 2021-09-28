package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import junit.framework.AssertionFailedError;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch.BatchType;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.web.JwtAuthenticationHelper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;

@Sql("classpath:seed.data/reset.sql")
@SqlMergeMode(MergeMode.MERGE)
@ActiveProfiles("message-integration")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class QueueIntegrationTest {

    public static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";
    public static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    protected MockWebServer hmppsAuthMock;
    protected MockWebServer prisonApiMock;
    protected MockWebServer pathfinderApiMock;
    protected MockWebServer communityApiMock;
    protected MockWebServer prisonRegisterMock;

    public void waitForPathFinderApiRequestTo(String url) {
        await().until(() -> requestExists(url));
    }

    public void waitUntilResponseQueueMessagesAreConsumed() {
        await().until(() -> getNumberOfMessagesCurrentlyOnSqsQueue(sqsResponseClientQueueUrl, sqsRequestClient) == 0);
    }

    int getNumberOfMessagesCurrentlyOnSqsQueue(String queueUrl, AmazonSQS client) {
        final GetQueueAttributesResult queueAttributes = client.getQueueAttributes(queueUrl, List.of(APPROXIMATE_NUMBER_OF_MESSAGES));
        return Integer.parseInt(queueAttributes.getAttributes().get(APPROXIMATE_NUMBER_OF_MESSAGES));
    }


    public boolean requestExists(String url) {
        try {
            return pathfinderApiMock.takeRequest().getRequestUrl().toString().contains(url);
        } catch (InterruptedException e) {
            throw new AssertionFailedError();
        }
    }

     MockResponse mockTokenAuthenticationResponse() {
        return new MockResponse()
            .setResponseCode(200)
            .setBody("{\"access_token\":\"123\",\"token_type\":\"bearer\",\"expires_in\":\"999999\"}")
            .setHeader("Content-Type", "application/json");
    }

     OffenderDeletionBatch persistNewBatch() {
        return batchRepository.save(OffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .referralCompletionDateTime(NOW.plusSeconds(1))
            .windowStartDateTime(NOW.plusSeconds(2))
            .windowEndDateTime(NOW.plusSeconds(3))
            .batchType(SCHEDULED)
            .build());
    }


    DeceasedOffenderDeletionBatch persistNewDeceasedOffenderBatch() {
        return deceasedOffenderDeletionBatch.save(DeceasedOffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .referralCompletionDateTime(NOW.plusSeconds(1))
            .batchType(BatchType.SCHEDULED)
            .build());
    }

     String waitUntilResolutionStatusIsPersisted(Long referralId, String status) {
        return new TransactionTemplate(transactionManager).execute(f -> {
            Awaitility.await().until(() -> offenderDeletionReferralRepository.findById(referralId).get().getReferralResolution().get().getResolutionStatus().name().equals(status));
            return null;
        });
    }

     String waitUntilResolutionStatusIsPersisted(String offenderId, String status) {
        return new TransactionTemplate(transactionManager).execute(f -> {
            Awaitility.await().until(() -> offenderDeletionReferralRepository.findByOffenderNo(offenderId).get(0).getReferralResolution().get().getResolutionStatus().name().equals(status));
            return null;
        });
    }

    List<DeceasedOffenderDeletionReferral> retrieveDeceasedReferralWithWait(String offenderNumber) {
        return new TransactionTemplate(transactionManager).execute(f -> {
            Awaitility.await().until(() -> Streamable.of(deceasedOffenderDeletionReferralRepository.findAll()).toList().size() == 1);
            return deceasedOffenderDeletionReferralRepository.findByOffenderNo(offenderNumber);
        });
    }

    protected void mockExternalServiceResponseCode(final int status) {
        var response = new MockResponse()
            .setResponseCode(status)
            .setBody(status == 200 ? "{\"status\": \"UP\"}" : "some error");

        prisonApiMock.enqueue(response);
        hmppsAuthMock.enqueue(response);
        pathfinderApiMock.enqueue(response);
        communityApiMock.enqueue(response);
        prisonRegisterMock.enqueue(response);
    }

    @Value("${data.compliance.response.sqs.queue.name}")
    String sqsResponseQueueName;

    @Value("${data.compliance.request.sqs.queue.name}")
    String sqsRequestQueueName;

    @Autowired
    @Qualifier("dataComplianceRequestSqsClient")
    AmazonSQS sqsRequestClient;

    @Autowired
    @Qualifier("dataComplianceRequestSqsDlqClient")
    AmazonSQS sqsRequestDlqClient;

    @Autowired
    @Qualifier("dataComplianceResponseSqsClient")
    AmazonSQS sqsResponseClient;

    @Autowired
    @Qualifier("dataComplianceResponseSqsDlqClient")
    AmazonSQS sqsResponseDlqClient;


    @Autowired
    @Qualifier("sqsRequestQueueUrl")
    public String sqsRequestClientQueueUrl;

    @Autowired
    @Qualifier("sqsRequestDlqQueueUrl")
    public String sqsRequestDlqClientQueueUrl;

    @Autowired
    @Qualifier("sqsResponseQueueUrl")
    public String sqsResponseClientQueueUrl;

    @Autowired
    @Qualifier("sqsResponseDlqQueueUrl")
    public String sqsResponseDlqClientQueueUrl;

    @Autowired
    MockJmsListener mockJmsListener;

    @Autowired
    OffenderDeletionBatchRepository batchRepository;

    @Autowired
    DeceasedOffenderDeletionBatchRepository deceasedOffenderDeletionBatch;

    @Autowired
    OffenderDeletionReferralRepository offenderDeletionReferralRepository;

    @Autowired
    DeceasedOffenderDeletionReferralRepository deceasedOffenderDeletionReferralRepository;

    @Autowired
    JwtAuthenticationHelper jwtAuthenticationHelper;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    protected WebTestClient webTestClient;


    @BeforeAll
    public static void setupAll(){
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Duration.ofMinutes(1));
    }

    @BeforeEach
    protected void setUp() throws Exception {
        hmppsAuthMock = new MockWebServer();
        hmppsAuthMock.start(8999);
        prisonApiMock = new MockWebServer();
        prisonApiMock.start(8998);
        pathfinderApiMock = new MockWebServer();
        pathfinderApiMock.start(8997);
        communityApiMock = new MockWebServer();
        communityApiMock.start(8996);
        prisonRegisterMock = new MockWebServer();
        prisonRegisterMock.start(8995);

        mockJmsListener.clearMessages();
    }

    @AfterEach
    protected  void tearDown() throws Exception {
        prisonApiMock.shutdown();
        hmppsAuthMock.shutdown();
        pathfinderApiMock.shutdown();
        communityApiMock.shutdown();
        prisonRegisterMock.shutdown();
    }


}
