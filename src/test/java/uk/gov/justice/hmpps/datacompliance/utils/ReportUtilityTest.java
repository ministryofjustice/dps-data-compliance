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
            .hasSize(98);

        assertThat(ualOffenders)
            .extracting(UalOffender::getNomsId)
            .contains("J4474JV", "J7077JM", "J5252JS", "J5252JS", "N/K");

        assertThat(ualOffenders)
            .extracting(UalOffender::getPrisonNumber)
            .contains("VR4298", "JL9912", "WJ8700", "Y26125", "WX8848");

        assertThat(ualOffenders)
            .extracting(UalOffender::getCroPnc)
            .contains("6211/95V", "38991/06V", "225734/99H", "59413/08S");
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
