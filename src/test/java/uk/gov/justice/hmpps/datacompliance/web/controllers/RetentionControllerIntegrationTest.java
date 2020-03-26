package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.OffenderRetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReason;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionRequest;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class RetentionControllerIntegrationTest extends IntegrationTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 1, 2, 3, 4, 5);

    @MockBean
    private TimeSource timeSource;

    @SpyBean
    private OffenderRetentionService retentionService;

    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;

    @Test
    @Sql("retention_reason_code.sql")
    void getRetentionReasons() {

        webTestClient.get().uri("/retention/offenders/retention-reasons")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(
                        "[" +
                                "{\"reasonCode\":\"CHILD_SEX_ABUSE\",\"displayName\":\"Child Sex Abuse Moratorium\",\"allowReasonDetails\":false}," +
                                "{\"reasonCode\":\"HIGH_PROFILE\",\"displayName\":\"High Profile Offenders\",\"allowReasonDetails\":false}," +
                                "{\"reasonCode\":\"LITIGATION_DISPUTE\",\"displayName\":\"Litigation/Dispute\",\"allowReasonDetails\":false}," +
                                "{\"reasonCode\":\"LOOKED_AFTER_CHILDREN\",\"displayName\":\"Looked after children\",\"allowReasonDetails\":false}," +
                                "{\"reasonCode\":\"MAPPA\",\"displayName\":\"MAPPA (Multi-Agency Public Protection Agreement)\",\"allowReasonDetails\":false}," +
                                "{\"reasonCode\":\"FOI_SAR\",\"displayName\":\"Subject to FOI/SARs\",\"allowReasonDetails\":false}," +
                                "{\"reasonCode\":\"OTHER\",\"displayName\":\"Other\",\"allowReasonDetails\":true}" +
                        "]");
    }

    @Test
    void getRetentionReasonsWithBadTokenIsUnauthorised() {

        webTestClient.get().uri("/retention/offenders/retention-reasons")
                .header("Authorization", "Bearer BAD.TOK.EN")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getRetentionReasonsWithNoAuthorizationHeaderIsUnauthorised() {

        webTestClient.get().uri("/retention/offenders/retention-reasons")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Sql("retention_reason_code.sql")
    @Sql("manual_retention.sql")
    @Sql("manual_retention_reason.sql")
    void getRetentionRecord() {

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"1\"")
                .expectBody().json(
                        "{" +
                                "\"offenderNo\":\"A1234BC\"," +
                                "\"userId\":\"user1\"," +
                                "\"modifiedDateTime\":\"1970-01-01T00:00:00\"," +
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

        when(retentionService.findManualOffenderRetention(new OffenderNumber("A1234BC")))
                .thenThrow(new RuntimeException("error!"));

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

    @Test
    @Sql("retention_reason_code.sql")
    void createRetentionRecord() {

        when(timeSource.nowAsLocalDateTime()).thenReturn(TIMESTAMP);

        final var request = ManualRetentionRequest.builder()
                .retentionReason(ManualRetentionReason.builder()
                        .reasonCode(ManualRetentionReasonCode.HIGH_PROFILE)
                        .reasonDetails("High profile for some reason")
                        .build())
                .build();

        webTestClient.put().uri("/retention/offenders/A1234BC")
                .bodyValue(request)
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/retention/offenders/A1234BC")
                .expectBody().isEmpty();

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"0\"")
                .expectBody().json(
                "{" +
                        "\"offenderNo\":\"A1234BC\"," +
                        "\"userId\":\"data-compliance-user\"," +
                        "\"modifiedDateTime\":\"2020-01-02T03:04:05\"," +
                        "\"retentionReasons\":[" +
                        "{\"reasonCode\":\"HIGH_PROFILE\",\"reasonDetails\":\"High profile for some reason\"}" +
                        "]" +
                "}");
    }

    @Test
    @Sql("retention_reason_code.sql")
    @Sql("manual_retention.sql")
    @Sql("manual_retention_reason.sql")
    void updateRetentionRecord() {

        when(timeSource.nowAsLocalDateTime()).thenReturn(TIMESTAMP);

        final var request = ManualRetentionRequest.builder()
                .retentionReason(ManualRetentionReason.builder()
                        .reasonCode(ManualRetentionReasonCode.OTHER)
                        .reasonDetails("Some other reason")
                        .build())
                .build();

        webTestClient.put().uri("/retention/offenders/A1234BC")
                .bodyValue(request)
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .header("If-Match", "\"1\"")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        webTestClient.get().uri("/retention/offenders/A1234BC")
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("ETag", "\"2\"")
                .expectBody().json(
                "{" +
                        "\"offenderNo\":\"A1234BC\"," +
                        "\"userId\":\"data-compliance-user\"," +
                        "\"modifiedDateTime\":\"2020-01-02T03:04:05\"," +
                        "\"retentionReasons\":[" +
                        "{\"reasonCode\":\"OTHER\",\"reasonDetails\":\"Some other reason\"}" +
                        "]" +
                "}");
    }

    @Test
    @Sql("retention_reason_code.sql")
    @Sql("manual_retention.sql")
    @Sql("manual_retention_reason.sql")
    void updateRetentionRecordReturnsBadRequestWhenIfMatchMissing() {

        webTestClient.put().uri("/retention/offenders/A1234BC")
                .bodyValue(ManualRetentionRequest.builder().build())
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                        "{" +
                                "\"status\":400," +
                                "\"userMessage\":\"Client Error\"," +
                                "\"developerMessage\":\"Must provide 'If-Match' header\"" +
                        "}");
    }

    @Test
    @Sql("retention_reason_code.sql")
    @Sql("manual_retention.sql")
    @Sql("manual_retention_reason.sql")
    void updateRetentionRecordReturnsBadRequestWhenExistingVersionDoesNotMatch() {

        webTestClient.put().uri("/retention/offenders/A1234BC")
                .bodyValue(ManualRetentionRequest.builder().build())
                .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
                .header("If-Match", "\"0\"")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                "{" +
                        "\"status\":400," +
                        "\"userMessage\":\"Client Error\"," +
                        "\"developerMessage\":\"Attempting to update an old version of the retention record\"" +
                        "}");
    }

    @Test
    void updateRetentionRecordWithBadTokenIsUnauthorised() {

        webTestClient.put().uri("/retention/offenders/A1234BC")
                .bodyValue(ManualRetentionRequest.builder().build())
                .header("Authorization", "Bearer BAD.TOK.EN")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateRetentionRecordWithNoAuthorizationHeaderIsUnauthorised() {

        webTestClient.put().uri("/retention/offenders/A1234BC")
                .bodyValue(ManualRetentionRequest.builder().build())
                .exchange()
                .expectStatus().isUnauthorized();
    }}
