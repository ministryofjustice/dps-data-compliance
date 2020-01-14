package uk.gov.justice.hmpps.datacompliance.services.health;

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

    static MockWebServer elite2ApiMock;
    static MockWebServer oauthApiMock;

    @BeforeAll
    static void setUp() throws Exception {
        elite2ApiMock = new MockWebServer();
        elite2ApiMock.start(8999);
        oauthApiMock = new MockWebServer();
        oauthApiMock.start(8998);
    }

    void mockExternalServiceResponseCode(final int status) {
        var response = new MockResponse()
                .setResponseCode(status)
                .setBody(status == 200 ? "pong" : "some error");

        elite2ApiMock.enqueue(response);
        oauthApiMock.enqueue(response);
    }

    @AfterAll
    static void tearDown() throws Exception {
        elite2ApiMock.shutdown();
        oauthApiMock.shutdown();
    }

    @Autowired
    WebTestClient webTestClient;
}
