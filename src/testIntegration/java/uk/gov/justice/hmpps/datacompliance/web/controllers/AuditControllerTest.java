package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.utils.web.JwtAuthenticationHelper;

public class AuditControllerTest  extends IntegrationTest {


    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;


    @Test
    void shouldGetDestructionLogWhenNoDataExists(){
        webTestClient.get().uri("/audit/destruction-log/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
            "[]");
    }


    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    @Sql("classpath:seed.data/image_duplicate.sql")
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    @Sql("classpath:seed.data/retention_reason_image_duplicate.sql")
    void shouldGetDestructionLog(){

        webTestClient.get().uri("/audit/destruction-log/")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
             "[{\"nomsId\":\"A1234AA\"," +
            "\"firstName\":\"John\"," +
            "\"middleName\":\"Middle\"," +
            "\"lastName\":\"Smith\"," +
            "\"dateOfBirth\":\"1969-01-01\"," +
            "\"typeOfRecordDestroyed\":\"NOMIS record\"," +
            "\"destructionDate\":\"2021-02-03T04:05:06\"," +
            "\"methodOfDestruction\":\"NOMIS database deletion\"," +
            "\"authorisationOfDestruction\":\"MOJ\"}," +
            "{\"nomsId\":\"C8841BD\"," +
            "\"firstName\":\"Jake\"," +
            "\"middleName\":\"Lee\"," +
            "\"lastName\":\"Rad\"," +
            "\"dateOfBirth\":\"1998-09-02\"," +
            "\"typeOfRecordDestroyed\":\"NOMIS record\"," +
            "\"destructionDate\":\"2021-07-06T04:05:06\"," +
            "\"methodOfDestruction\":\"NOMIS database deletion\"," +
            "\"authorisationOfDestruction\":\"MOJ\"}]");
    }

    @Test
    void shouldFailGetDestructionLogWhenUnauthorised(){
        webTestClient.get().uri("/audit/destruction-log/")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();

    }

    @Test
    void x(){
        webTestClient.get().uri("/audit/destruction-log/pdf")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_PDF)
            .exchange()
            .expectStatus().isOk();

    }



}
