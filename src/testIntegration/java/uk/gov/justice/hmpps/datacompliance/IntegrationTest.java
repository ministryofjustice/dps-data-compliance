package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.web.reactive.server.WebTestClient;

@Sql("classpath:seed.data/reset.sql")
@SqlMergeMode(MergeMode.MERGE)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {

    protected static MockWebServer hmppsAuthMock;
    protected static MockWebServer prisonApiMock;
    protected static MockWebServer pathfinderApiMock;

    @Autowired
    protected WebTestClient webTestClient;

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

}
