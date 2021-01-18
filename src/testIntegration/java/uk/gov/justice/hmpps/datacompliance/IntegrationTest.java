package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.gov.justice.hmpps.datacompliance.config.JmsLocalStackConfig;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Sql("classpath:seed.data/reset.sql")
@SqlMergeMode(MergeMode.MERGE)
@ContextConfiguration(classes = JmsLocalStackConfig.class)
public abstract class IntegrationTest {

    protected static MockWebServer hmppsAuthMock;
    protected static MockWebServer prisonApiMock;
    protected static MockWebServer pathfinderApiMock;

    @BeforeAll
    protected static void setUp() throws Exception {
        hmppsAuthMock = new MockWebServer();
        hmppsAuthMock.start(8999);
        prisonApiMock = new MockWebServer();
        prisonApiMock.start(8998);
        pathfinderApiMock = new MockWebServer();
        pathfinderApiMock.start(8997);
    }

    protected void mockExternalServiceResponseCode(final int status) {
        var response = new MockResponse()
            .setResponseCode(status)
            .setBody(status == 200 ? "pong" : "some error");

        prisonApiMock.enqueue(response);
        hmppsAuthMock.enqueue(response);
        pathfinderApiMock.enqueue(response);
    }

    @AfterAll
    protected static void tearDown() throws Exception {
        prisonApiMock.shutdown();
        hmppsAuthMock.shutdown();
        pathfinderApiMock.shutdown();
    }

    @Autowired
    protected WebTestClient webTestClient;

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
    AmazonSNS awsSnsClient;

}
