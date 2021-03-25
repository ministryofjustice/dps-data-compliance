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
            "[{\"nomsId\":\"N/K\",\"prisonNumber\":\"EN9883\",\"croPnc\":\"35/971011Q\",\"firstNames\":\"BNTP45GNPLLB\",\"familyName\":\"FERGREGWF\",\"indexOffenceDescription\":\"wefewbgEFWFewf eTRBwwefewwev fR/TBewfehjm\"}," +
                "{\"nomsId\":\"B1571LL\",\"prisonNumber\":\"QR1711\",\"croPnc\":\"11111/37U\",\"firstNames\":\"FEWGRTHHJ\",\"familyName\":\"SmWTAFESREGXtB\",\"indexOffenceDescription\":\"EFWFSPsgrTReung sbLXng(dTRH fdfdf)ewfew\"}," +
                "{\"nomsId\":\"N/K\",\"prisonNumber\":\"ON9134\",\"croPnc\":\"05/971311Q\",\"firstNames\":\"WEREWR43\",\"familyName\":\"TPLDBLB\",\"indexOffenceDescription\":\"EWFbgFEWE FEWF EWwefewwe vfR/TBew fehjm\"}," +
                "{\"nomsId\":\"P9824PP\",\"prisonNumber\":\"ED8830\",\"croPnc\":\"13111/77U\",\"firstNames\":\"FrPRGGETL\",\"familyName\":\"EWFAFW\",\"indexOffenceDescription\":\"SEFEWFWERF WERFEW(ADFdfEWF)ewfFEEWFEWEFew\"}," +
                "{\"nomsId\":\"N/K\",\"prisonNumber\":\"MN9183\",\"croPnc\":\"05/971311Q\",\"firstNames\":\"BNTP45GNPLLB\",\"familyName\":\"FERGREGWF\",\"indexOffenceDescription\":\"wefewbgEFW FewfeTRBwwefew wevfR/TBewfehjm\"}," +
                "{\"nomsId\":\"N/K\",\"prisonNumber\":\"EN8134\",\"croPnc\":\"35/973011Q\",\"firstNames\":\"WEREWR43\",\"familyName\":\"TPLDBLB\",\"indexOffenceDescription\":\"EWFbgFE WEFEWFEWwefewwevfR /TBewfe hjm\"}," +
                "{\"nomsId\":\"\",\"prisonNumber\":\"PD2830\",\"croPnc\":\"11131/73U\",\"firstNames\":\"FrPRGGETL\",\"familyName\":\"EWFAFW\",\"indexOffenceDescription\":\"SEFEWFWERF WERFEW(ADFdfEWF )ewfFEEWFEWEFew\"}," +
                "{\"nomsId\":\"N/K\",\"prisonNumber\":\"MN9183\",\"croPnc\":\"05/371011Q\",\"firstNames\":\"BNTP45GNPLLB\",\"familyName\":\"FERGREGWF\",\"indexOffenceDescription\":\"wef ewbgEFWFewfeTRBwwefe wwevfR/TBewfehjm\"}" +
                ",{\"nomsId\":\"\",\"prisonNumber\":\"QR1781\",\"croPnc\":\"11311/73U\",\"firstNames\":\"FEWGRTHHJ\",\"familyName\":\"SmWTAFESREGXtB\",\"indexOffenceDescription\":\"EFWFSPsgrTReun gsbLXng(dTRHfdfdf)ewfew\"}]");
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
