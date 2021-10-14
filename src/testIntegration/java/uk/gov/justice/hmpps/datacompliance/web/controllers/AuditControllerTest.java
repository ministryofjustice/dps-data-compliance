package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.utils.web.JwtAuthenticationHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditControllerTest extends IntegrationTest {


    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;

    @Test
    void shouldFailGetDestructionLogWhenUnauthorised() {
        webTestClient.get().uri("/audit/destruction-log/")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();

    }

    @Test
    void shouldGetDestructionLogWhenNoDataExists() {
        webTestClient.get().uri("/audit/destruction-log/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                "{\"content\":[],\"pageable\":\"INSTANCE\",\"totalPages\":1,\"last\":true,\"totalElements\":0,\"size\":0,\"number\":0,\"sort\":{\"empty\":true,\"unsorted\":true,\"sorted\":false},\"first\":true,\"numberOfElements\":0,\"empty\":true}");
    }


    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldGetDestructionLog() {

        webTestClient.get().uri("/audit/destruction-log/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                """
                    {"content":[{"NOMIS_ID":"C8841BD",
                    "FIRST_NAME":"Jake",
                    "MIDDLE_NAME":"Lee",
                    "LAST_NAME":"Rad",
                    "DATE_OF_BIRTH":"1998-09-02",
                    "LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT":"LXH",
                    "TYPE_OF_RECORD_DESTROYED":"NOMIS record",
                    "DESTRUCTION_DATE":"2021-07-06 04:05:06",
                    "METHOD_OF_DESTRUCTION":"NOMIS database deletion",
                    "AUTHORISATION_OF_DESTRUCTION":"MOJ"},
                        {"NOMIS_ID":"D8950VX",
                        "FIRST_NAME":"Lucy",
                        "MIDDLE_NAME":"Liam",
                        "LAST_NAME":"Oliver",
                        "DATE_OF_BIRTH":"1987-10-02",
                        "LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT":"HMD",
                        "TYPE_OF_RECORD_DESTROYED":"NOMIS record",
                        "DESTRUCTION_DATE":"2000-01-06 04:05:06",
                        "METHOD_OF_DESTRUCTION":"NOMIS database deletion",
                        "AUTHORISATION_OF_DESTRUCTION":"MOJ"}],
                        "pageable":"INSTANCE","last":true,"totalElements":2,"totalPages":1,"size":2,"number":0,"sort":{"empty":true,"unsorted":true,"sorted":false},"first":true,"numberOfElements":2,"empty":false}
                    """);
    }


    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldGetDestructionLogAsCsv() {
        webTestClient.get().uri("/audit/destruction-log")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.valueOf("text/csv"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("text/csv")
            .returnResult(String.class).consumeWith(response -> {
                assertThat(response.getResponseBody()).isNotNull();
                assertThat(response.getResponseBody().blockFirst())
                    .contains("""
                        NOMIS_ID,FIRST_NAME,MIDDLE_NAME,LAST_NAME,DATE_OF_BIRTH,"LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT",TYPE_OF_RECORD_DESTROYED,DESTRUCTION_DATE,METHOD_OF_DESTRUCTION,"AUTHORISATION_OF_DESTRUCTION""");
            });

    }

    @Test
    void shouldGetDestructionLogAsCsvWhenNoDataExists() {
        webTestClient.get().uri("/audit/destruction-log")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.valueOf("text/csv"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("text/csv")
            .expectBody().isEmpty();
    }


    @Test
    void shouldFailGetRetainedOffendersWhenUnauthorised() {
        webTestClient.get().uri("/audit/retained-offenders/")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();

    }

    @Test
    void shouldGetRetainedOffendersWhenNoDataExists() {
        webTestClient.get().uri("/audit/retained-offenders/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                "{\"content\":[],\"pageable\":\"INSTANCE\",\"totalPages\":1,\"last\":true,\"totalElements\":0,\"size\":0,\"number\":0,\"sort\":{\"empty\":true,\"unsorted\":true,\"sorted\":false},\"first\":true,\"numberOfElements\":0,\"empty\":true}");
    }


    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldGetRetainedOffenders() {

        webTestClient.get().uri("/audit/retained-offenders/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                """
                    {"content":
                    [{"NOMIS_ID":"A1234AA",
                    "FIRST_NAME":"John",
                    "MIDDLE_NAME":"Middle",
                    "LAST_NAME":"Smith",
                    "DATE_OF_BIRTH":"1969-01-01",
                    "LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT":"LEI",
                    "RETENTION_REASONS":"PATHFINDER_REFERRAL"}],
                    "pageable":"INSTANCE","last":true,"totalElements":1,"totalPages":1,"size":1,"number":0,"sort":{"empty":true,"unsorted":true,"sorted":false},"first":true,"numberOfElements":1,"empty":false}
                    """);
    }


    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check_3.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldGetRetainedOffenderDuplicates() {

        webTestClient.get().uri("/audit/retained-offenders?filter=duplicates")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                """
                    {"content":
                    [{"NOMIS_ID":"A1234AA",
                    "FIRST_NAME":"John",
                    "MIDDLE_NAME":"Middle",
                    "LAST_NAME":"Smith",
                    "DATE_OF_BIRTH":"1969-01-01",
                    "LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT":"LEI",
                    "RETENTION_REASONS":"DATA_DUPLICATE_AP & DATA_DUPLICATE_ID & IMAGE_DUPLICATE"}],
                    "pageable":"INSTANCE","last":true,"totalElements":1,"totalPages":1,"size":1,"number":0,"sort":{"empty":true,"unsorted":true,"sorted":false},"first":true,"numberOfElements":1,"empty":false}
                    """);
    }

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check_3.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldGetRetainedOffendersAsCsv() {
        webTestClient.get().uri("/audit/retained-offenders/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.valueOf("text/csv"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("text/csv")
            .returnResult(String.class).consumeWith(response -> {
                assertThat(response.getResponseBody()).isNotNull();
                assertThat(response.getResponseBody().blockFirst())
                    .contains("""
                        NOMIS_ID,FIRST_NAME,MIDDLE_NAME,LAST_NAME,DATE_OF_BIRTH,"LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT",RETENTION_REASONS""");
            });

    }

    @Test
    void shouldGetRetainedOffendersAsCsvWhenNoDataExists() {
        webTestClient.get().uri("/audit/retained-offenders/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.valueOf("text/csv"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("text/csv")
            .expectBody().isEmpty();
    }

}
