package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import junit.framework.AssertionFailedError;
import okhttp3.mockwebserver.MockResponse;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.web.JwtAuthenticationHelper;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;

public class QueueIntegrationTest extends IntegrationTest {

    public static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";

    public static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

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
        return repository.save(OffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .referralCompletionDateTime(NOW.plusSeconds(1))
            .windowStartDateTime(NOW.plusSeconds(2))
            .windowEndDateTime(NOW.plusSeconds(3))
            .batchType(SCHEDULED)
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

    @Value("${data.compliance.response.sqs.queue.name}")
    String sqsResponseQueueName;

    @Value("${data.compliance.request.sqs.queue.name}")
    String sqsRequestQueueName;

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
    OffenderDeletionBatchRepository repository;

    @Autowired
    OffenderDeletionReferralRepository offenderDeletionReferralRepository;

    @Autowired
    JwtAuthenticationHelper jwtAuthenticationHelper;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    public void clearMockJmsListener(){
        mockJmsListener.clearMessages();
    }


}
