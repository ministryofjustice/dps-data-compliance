package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.localstack.LocalStackContainer;
import uk.gov.justice.hmpps.datacompliance.config.LocalStackConfig;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch.BatchType;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.DeceasedOffenderDeletionResult;
import uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.DeceasedOffenderDeletionResult.DeceasedOffender;
import uk.gov.justice.hmpps.sqs.HmppsQueueService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Sql("classpath:seed.data/reset.sql")
@SqlMergeMode(MergeMode.MERGE)
@ActiveProfiles("test-dlq")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class QueueIntegrationTest {

    public static final LocalStackContainer localStackContainer;
    public static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    protected ObjectMapper objectMapper;
    protected AmazonSQS sqsRequestClient;
    protected AmazonSQS sqsResponseClient;
    protected String sqsResponseClientQueueUrl;
    protected String sqsRequestClientQueueUrl;
    protected AmazonSQS sqsDlqRequestClient;
    protected AmazonSQS sqsDlqResponseClient;
    protected String sqsDlqResponseClientQueueUrl;
    protected String sqsDlqRequestClientQueueUrl;


    static {
        localStackContainer = LocalStackConfig.instance();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (localStackContainer != null) {
            LocalStackConfig.setLocalStackProperties(localStackContainer, registry);
        }
    }


    protected int getNumberOfMessagesOnRequestQueue() {
        return getNumberOfMessagesCurrentlyOnSqsQueue(sqsRequestClientQueueUrl, sqsRequestClient);
    }

    protected int getNumberOfMessagesOnDlqRequestQueue() {
        return getNumberOfMessagesCurrentlyOnSqsQueue(sqsDlqRequestClientQueueUrl, sqsDlqRequestClient);
    }

    protected int getNumberOfMessagesOnDlqResponseQueue() {
        return getNumberOfMessagesCurrentlyOnSqsQueue(sqsDlqResponseClientQueueUrl, sqsDlqResponseClient);
    }

    int getNumberOfMessagesCurrentlyOnSqsQueue(String queueUrl, AmazonSQS client) {
        final GetQueueAttributesResult queueAttributes = client.getQueueAttributes(queueUrl, List.of("ApproximateNumberOfMessages"));
        return Integer.parseInt(queueAttributes.getAttributes().get("ApproximateNumberOfMessages"));
    }


    DeceasedOffenderDeletionBatch persistNewDeceasedOffenderBatch() {
        return deceasedOffenderDeletionBatch.save(DeceasedOffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .referralCompletionDateTime(NOW.plusSeconds(1))
            .batchType(BatchType.SCHEDULED)
            .build());
    }


    List<DeceasedOffenderDeletionReferral> retrieveDeceasedReferralWithWait(String offenderNumber) {
        return new TransactionTemplate(transactionManager).execute(f -> {
            Awaitility.await().until(() -> Streamable.of(deceasedOffenderDeletionReferralRepository.findAll()).toList().size() == 1);
            return deceasedOffenderDeletionReferralRepository.findByOffenderNo(offenderNumber);
        });
    }


    @Autowired
    HmppsQueueService hmppsQueueService;

    @Autowired
    DeceasedOffenderDeletionBatchRepository deceasedOffenderDeletionBatch;


    @Autowired
    DeceasedOffenderDeletionReferralRepository deceasedOffenderDeletionReferralRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    protected WebTestClient webTestClient;

    @LocalServerPort
    protected Integer port = 0;


    @BeforeAll
    protected static void setupAll() {
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Duration.ofMinutes(1));
    }

    @BeforeEach
    protected void setUp() {
        initaliseQueueProperties();
    }

    private void initaliseQueueProperties() {
        final var requestQueue = hmppsQueueService.findByQueueId("datacompliancerequest");
        sqsRequestClient = requestQueue.getSqsClient();
        sqsRequestClientQueueUrl = requestQueue.getQueueUrl();

        final var responseQueue = hmppsQueueService.findByQueueId("datacomplianceresponse");
        sqsResponseClient = responseQueue.getSqsClient();
        sqsResponseClientQueueUrl = responseQueue.getQueueUrl();

        sqsDlqRequestClient = requestQueue.getSqsDlqClient();
        sqsDlqRequestClientQueueUrl = sqsDlqRequestClient.getQueueUrl(requestQueue.getDlqName()).getQueueUrl();

        sqsDlqResponseClient = responseQueue.getSqsDlqClient();
        sqsDlqResponseClientQueueUrl = sqsResponseClient.getQueueUrl(responseQueue.getDlqName()).getQueueUrl();
    }

    protected SendMessageRequest forDeceasedOffenderDeletionResult(String queueUrl, Long batchId, DeceasedOffender deceasedOffender) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                "eventType", stringAttribute("DATA_COMPLIANCE_DECEASED-OFFENDER-DELETION-RESULT"),
                "contentType", stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(DeceasedOffenderDeletionResult.builder()
                .batchId(batchId)
                .deceasedOffender(deceasedOffender)
                .build()));
    }

    protected ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return objectMapper;
    }
    protected MessageAttributeValue stringAttribute(final String value) {
        return new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(value);
    }

    protected  DeceasedOffender getDeceasedOffender(String offenderIdDisplay) {
        return DeceasedOffender.builder()
            .offenderIdDisplay(offenderIdDisplay)
            .firstName("someFirstName")
            .middleName("someMiddleName")
            .lastName("someLastName")
            .agencyLocationId("someAgencyLocationId")
            .birthDate(LocalDate.now().minusYears(30))
            .deceasedDate(LocalDate.now().minusYears(1))
            .deletionDateTime(LocalDateTime.now().minusMinutes(1))
            .build();
    }

    protected String asJson(Object value) {
        try {
            return getObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}