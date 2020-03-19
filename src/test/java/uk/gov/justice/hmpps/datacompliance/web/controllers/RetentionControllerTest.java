package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.OffenderRetentionService;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReason;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode.HIGH_PROFILE;

class RetentionControllerTest extends IntegrationTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 1, 2, 3, 4, 5);

    @MockBean
    private OffenderRetentionService retentionService;

    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;

    @Test
    void getRetentionRecord() {

        mockRetentionRecord();

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(
                        "{" +
                                "\"offenderNo\":\"A1234BC\"," +
                                "\"staffId\":1234," +
                                "\"modifiedDateTime\":\"2020-01-02T03:04:05\"," +
                                "\"retentionReasons\":[" +
                                "{\"reasonCode\":\"HIGH_PROFILE\",\"reasonDetails\":\"High profile for some reason\"}" +
                                "]" +
                        "}");
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

    @Test
    void getRetentionRecordReturnsNotFound() {

        when(retentionService.findManualOffenderRetention(any())).thenReturn(Optional.empty());

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().json(
                        "{" +
                                "\"status\":404," +
                                "\"userMessage\":\"Entity Not Found\"" +
                        "}");
    }

    @Test
    void getRetentionRecordReturnsInternalServerError() {

        when(retentionService.findManualOffenderRetention(any())).thenThrow(new RuntimeException("error!"));

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().json(
                        "{" +
                                "\"status\":500," +
                                "\"userMessage\":\"Internal Server Error\"," +
                                "\"developerMessage\":\"error!\"" +
                        "}");
    }

    private void mockRetentionRecord() {
        when(retentionService.findManualOffenderRetention(new OffenderNumber("A1234BC")))
                .thenReturn(Optional.of(ManualRetention.builder()
                        .offenderNo("A1234BC")
                        .staffId(1234L)
                        .modifiedDateTime(TIMESTAMP)
                        .retentionReason(ManualRetentionReason.builder()
                                .reasonCode(HIGH_PROFILE)
                                .reasonDetails("High profile for some reason")
                                .build())
                        .build()));
    }
}
