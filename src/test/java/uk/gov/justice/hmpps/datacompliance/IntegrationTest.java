package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration
public class IntegrationTest {

    protected static MockWebServer oauthApiMock;
    protected static MockWebServer elite2ApiMock;
    protected static MockWebServer pathfinderApiMock;

    @BeforeAll
    protected static void setUp() throws Exception {
        oauthApiMock = new MockWebServer();
        oauthApiMock.start(8999);
        elite2ApiMock = new MockWebServer();
        elite2ApiMock.start(8998);
        pathfinderApiMock = new MockWebServer();
        pathfinderApiMock.start(8997);
    }

    protected void mockExternalServiceResponseCode(final int status) {
        var response = new MockResponse()
                .setResponseCode(status)
                .setBody(status == 200 ? "pong" : "some error");

        elite2ApiMock.enqueue(response);
        oauthApiMock.enqueue(response);
        pathfinderApiMock.enqueue(response);
    }

    @AfterAll
    protected static void tearDown() throws Exception {
        elite2ApiMock.shutdown();
        oauthApiMock.shutdown();
        pathfinderApiMock.shutdown();
    }

    @Autowired
    protected WebTestClient webTestClient;
}
