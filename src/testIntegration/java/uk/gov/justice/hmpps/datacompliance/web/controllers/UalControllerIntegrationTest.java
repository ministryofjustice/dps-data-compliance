package uk.gov.justice.hmpps.datacompliance.web.controllers;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;
import uk.gov.justice.hmpps.datacompliance.utils.web.JwtAuthenticationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UalControllerIntegrationTest extends IntegrationTest {

    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;

    @Autowired
    private OffenderUalRepository offenderUalRepository;

    private final byte[] EXAMPLE_UAL_REPORT = getReport();

    @Test
    public void getUalOffenderDataWhenNoDataExists() {

        webTestClient.get()
            .uri("/ual/offender-data")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("[]");

    }


    @Test
    @Sql("offender_ual.sql")
    public void getUalOffenderData() {

        webTestClient.get().uri("/ual/offender-data")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
            expectedJson());
    }

    @Test
    void updateUalReport() {

        webTestClient.put()
            .uri("/ual/report")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(fromFile(EXAMPLE_UAL_REPORT)))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader().valueEquals("Location", "/ual/offender-data")
            .expectBody().isEmpty();

    }

    @Test
    void updateUalReportWithReportThatDoesNotAdhereToCorrectFormat() {

        webTestClient.put()
            .uri("/ual/report")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(fromFile("Some,csv,file,that,does,not,adhere,to,the,correct,format.csv" .getBytes(StandardCharsets.UTF_8))))
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectBody().isEmpty();

    }

    @Test
    @Sql("offender_ual.sql")
    void shouldUpdateExistingEntriesAndRemoveSupersededEntries() {

        webTestClient.get().uri("/ual/offender-data")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
            expectedJson());

        webTestClient.put()
            .uri("/ual/report")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(fromFile(EXAMPLE_UAL_REPORT)))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader().valueEquals("Location", "/ual/offender-data")
            .expectBody().isEmpty();

        webTestClient.get().uri("/ual/offender-data")
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
            "[{\"nomsId\":\"\"," +
                "\"prisonNumber\":\"AB4944\"," +
                "\"croPnc\":\"95/94744V\"," +
                "\"firstNames\":\"APBn\"," +
                "\"familyName\":\"LULURAZ\"," +
                "\"indexOffenceDescription\":\"RPBBARZ\"}," +
                "{\"nomsId\":\"\"," +
                "\"prisonNumber\":\"AL4444\"," +
                "\"croPnc\":\"49/144544K\"," +
                "\"firstNames\":\"GArBrL\"," +
                "\"familyName\":\"MLBRABRTBZ\"" +
                ",\"indexOffenceDescription\":\"APSSASSIPN SXCL\"}," +
                "{\"nomsId\":\"\",\"prisonNumber\":\"ML4414\"," +
                "\"croPnc\":\"114477/95S\"," +
                "\"firstNames\":\"TBBLLAus\"," +
                "\"familyName\":\"LBMABALL\"," +
                "\"indexOffenceDescription\":\"LPNSAIRBLZ TP SUAALZ SXCLS\"}," +
                "{\"nomsId\":\"B7444BL\",\"prisonNumber\":\"AT0014\"," +
                "\"croPnc\":\"044774/74B\"," +
                "\"firstNames\":\"LPUIS ABMAS\"," +
                "\"familyName\":\"SBRNAR\"," +
                "\"indexOffenceDescription\":\"RPBBARZ\"}," +
                "{\"nomsId\":\"N/K\"," +
                "\"prisonNumber\":\"BA9444\"," +
                "\"croPnc\":\"444794/04M/0\"," +
                "\"firstNames\":\"SBZNA\"," +
                "\"familyName\":\"BANRZ\"," +
                "\"indexOffenceDescription\":\"SUAALZING SXCLS\"}," +
                "{\"nomsId\":\"N/K\"," +
                "\"prisonNumber\":\"MN9444\"," +
                "\"croPnc\":\"05/914044Q\"," +
                "\"firstNames\":\"BNTPNALLB\"," +
                "\"familyName\":\"TALLB\"," +
                "\"indexOffenceDescription\":\"APSSASSIPN SXCLS SITB INTANT TP SUAALZ\"}," +
                "{\"nomsId\":\"BR4799\"," +
                "\"prisonNumber\":\"TM9945\"," +
                "\"croPnc\":\"444419/00K\"," +
                "\"firstNames\":\"SusBn\"," +
                "\"familyName\":\"P'BRIAN\"," +
                "\"indexOffenceDescription\":\"RPBBARZ\"}," +
                "{\"nomsId\":\"B4154BB\"," +
                "\"prisonNumber\":\"RA5457\"," +
                "\"croPnc\":\"41074/05G\"," +
                "\"firstNames\":\"SPn TBBnB\"," +
                "\"familyName\":\"TRINB\"," +
                "\"indexOffenceDescription\":\"APSSASSIPN SXCLS SITB INTANT TP SUAALZ\"}," +
                "{\"nomsId\":\"B4574LL\"," +
                "\"prisonNumber\":\"LR4744\"," +
                "\"croPnc\":\"44444/77U\"," +
                "\"firstNames\":\"FrAL\"," +
                "\"familyName\":\"SmitB\"," +
                "\"indexOffenceDescription\":\"MBliLiPus SPunLing (INFLILTING GBB) s40\"}]");
    }


    @NotNull
    private String expectedJson() {
        return "[{\"nomsId\":\"A1234AA\",\"prisonNumber\":\"AR2788\",\"croPnc\":\"13862/77U\",\"firstNames\":\"John\",\"familyName\":\"Smith\",\"indexOffenceDescription\":\"Malicious Wounding (INFLICTING GBH) s20\"},{\"prisonNumber\":\"BR2799\",\"croPnc\":\"28691/44U\",\"firstNames\":\"Tom\",\"familyName\":\"Johnson\",\"indexOffenceDescription\":\"IMPORT/EXPORT/DRUG\"}," +
            "{\"nomsId\":\"C3234AA\",\"prisonNumber\":\"CR2788\",\"croPnc\":\"33862/77U\",\"firstNames\":\"Fred\",\"familyName\":\"Smith\",\"indexOffenceDescription\":\"Murder\"}," +
            "{\"nomsId\":\"D4891BB\",\"croPnc\":\"48691/44U\",\"firstNames\":\"Lucy\",\"familyName\":\"Johnson\",\"indexOffenceDescription\":\"Malicious Wounding (INFLICTING GBH) s20\"}]";
    }

    public MultiValueMap<String, HttpEntity<?>> fromFile(final byte[] report) {
        var builder = new MultipartBodyBuilder();
        var header = String.format("form-data; name=%s; filename=%s", "file", "fileName.csv");
        builder.part("file", new ByteArrayResource(report)).header("Content-Disposition", header);
        return builder.build();
    }

    private byte[] getReport() {
        try {
            return getClass().getClassLoader().getResourceAsStream("uk/gov/justice/hmpps/datacompliance/web/controllers/ual_offenders_data.csv").readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
