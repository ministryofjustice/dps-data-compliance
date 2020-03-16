package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;

class RetentionControllerTest extends IntegrationTest {

    // TODO GDPR-77 Secure API with JWT
    @Test
    void getRetentionRecordForbidden() {

        webTestClient.get().uri("/retention/offender/A1234BC")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden();
    }
}