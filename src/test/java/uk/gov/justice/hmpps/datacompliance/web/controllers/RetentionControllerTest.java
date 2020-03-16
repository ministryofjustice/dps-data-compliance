package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;

class RetentionControllerTest extends IntegrationTest {

    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;

    @Test
    void getRetentionRecord() {

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{}");
    }

    @Test
    void getRetentionRecordWithBadTokenIsUnauthorised() {

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer BAD.TOK.EN")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getRetentionRecordWithNoAuthorizationHeaderIsUnauthorised() {

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}