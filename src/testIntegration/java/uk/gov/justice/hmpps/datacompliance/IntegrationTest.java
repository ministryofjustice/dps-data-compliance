package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

@Sql("classpath:seed.data/reset.sql")
@SqlMergeMode(MergeMode.MERGE)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {

    protected MockWebServer hmppsAuthMock;
    protected MockWebServer prisonApiMock;
    protected MockWebServer pathfinderApiMock;

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    MockJmsListener mockJmsListener;

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
        mockJmsListener.clearMessages();
    }

    protected void mockExternalServiceResponseCode(final int status) {
        var response = new MockResponse()
            .setResponseCode(status)
            .setBody(status == 200 ? "pong" : "some error");

        prisonApiMock.enqueue(response);
        hmppsAuthMock.enqueue(response);
        pathfinderApiMock.enqueue(response);
    }

    @AfterEach
    protected  void tearDown() throws Exception {
        prisonApiMock.shutdown();
        hmppsAuthMock.shutdown();
        pathfinderApiMock.shutdown();
    }

}
