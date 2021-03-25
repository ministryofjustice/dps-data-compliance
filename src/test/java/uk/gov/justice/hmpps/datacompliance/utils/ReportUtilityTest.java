package uk.gov.justice.hmpps.datacompliance.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.hmpps.datacompliance.services.ual.UalOffender;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

class ReportUtilityTest {

    private final ReportUtility reportUtility = new ReportUtility();

    @Test
    public void parseFromUalReport() throws IOException {
        final var report = getClass().getClassLoader().getResourceAsStream("uk/gov/justice/hmpps/datacompliance/utils/ual_offenders_data.csv").readAllBytes();
        final var multipartFile = new MockMultipartFile("some.file", "file.csv", MULTIPART_FORM_DATA_VALUE, report);

        final var ualOffenders = reportUtility.parseFromUalReport(multipartFile);

        assertThat(ualOffenders)
            .isNotEmpty()
            .hasSize(10);

        assertThat(ualOffenders)
            .extracting(UalOffender::getNomsId)
            .contains("B1571LL", "N/K", "B1571LL");

        assertThat(ualOffenders)
            .extracting(UalOffender::getPrisonNumber)
            .contains("QR1711", "ON9134", "ED8830", "ED8830", "QR1711");

        assertThat(ualOffenders)
            .extracting(UalOffender::getCroPnc)
            .contains("35/971011Q", "05/971311Q", "35/973011Q", "11311/73U");

        assertThat(ualOffenders)
            .extracting(UalOffender::getFirstNames)
            .contains("BNTP45GNPLLB", "WEREWR43", "BNTP45GNPLLB", "FrPRGGETL");

        assertThat(ualOffenders)
            .extracting(UalOffender::getFamilyName)
            .contains("FERGREGWF", "TPLDBLB", "SmWTAFESREGXtB", "FERGREGWF");

        assertThat(ualOffenders)
            .extracting(UalOffender::getIndexOffenceDescription)
            .contains("SEFEWFWERF WERFEW(ADFdfEWF )ewfFEEWFEWEFew", "EFWFSPsgrTReun gsbLXng(dTRHfdfdf)ewfew", "wef ewbgEFWFewfeTRBwwefe wwevfR/TBewfehjm");
    }

    @Test
    public void parseFromUalReportInvalidExtension() throws IOException {
        final var report = getClass().getClassLoader().getResourceAsStream("uk/gov/justice/hmpps/datacompliance/utils/ual_offenders_data.csv").readAllBytes();
        final var multipartFile = new MockMultipartFile("some.file", "file.txt", MULTIPART_FORM_DATA_VALUE, report);

        assertThatThrownBy(() -> reportUtility.parseFromUalReport(multipartFile))
            .isInstanceOf(HttpClientErrorException.class)
            .hasMessage("400 The file must be a CSV with the .csv extension type.");
    }

    @Test
    public void parseFromUalReportInvalidFileFormat(){
        final var multipartFile = new MockMultipartFile("some.file", "file.csv", MULTIPART_FORM_DATA_VALUE, ":}{|(*)(*&%&*^£$%@£$@!%£&".getBytes(Charset.forName("EUC-JP")));

        assertThatThrownBy(() -> reportUtility.parseFromUalReport(multipartFile))
            .isInstanceOf(HttpClientErrorException.class)
            .hasMessage("400 Unable to parse file. Please provide a use a csv which adheres to the specified format. Allowed values are: nomsId, prisonNumber, croPnc, firstNames, familyName, indexOffenceDescription");
    }

}
