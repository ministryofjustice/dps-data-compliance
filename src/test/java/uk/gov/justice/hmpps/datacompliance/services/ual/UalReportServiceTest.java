package uk.gov.justice.hmpps.datacompliance.services.ual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.justice.hmpps.datacompliance.dto.UalOffender;
import uk.gov.justice.hmpps.datacompliance.dto.UalOffender.UalOffenderBuilder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity.OffenderUalEntityBuilder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.utils.ReportUtility;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse.UalOffenderResponseBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UalReportServiceTest {

    public static final long OFFENDER_ENTITY_ID = 1L;
    private static final String USERNAME = "user1";
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    private static final String OFFENDER_PNC = "1999/0123456X";
    private static final String FORMATTED_OFFENDER_PNC = "99/123456X";
    private static final String OFFENDER_CRO = "000001/11X";
    private static final String FORMATTED_OFFENDER_CRO = "11/1X";

    @Mock
    OffenderUalRepository offenderUalRepository;

    @Mock
    ReportUtility reportUtility;

    @Mock
    private UserSecurityUtils userSecurityUtils;

    private UalReportService reportService;

    @BeforeEach
    public void setUp() {
        reportService = new UalReportService(reportUtility, offenderUalRepository, TimeSource.of(NOW), userSecurityUtils);
    }

    @Test
    void getUalOffenders() {
        when(offenderUalRepository.findAll()).thenReturn(List.of(offenderEntity().build()));

        assertThat(reportService.getUalOffenders()).containsExactly(expectedUalOffenderResponse().build());
    }

    @Test
    void getUalOffendersWhenEmpty() {
        when(offenderUalRepository.findAll()).thenReturn(emptyList());

        assertThat(reportService.getUalOffenders()).isEmpty();
    }

    @Test
    void replaceReportWithPnc() {
        final var ualOffender = ualOffender().build();
        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        mockUsername();
        when(reportUtility.parseFromUalReport(any())).thenReturn(List.of(ualOffender));
        when(offenderUalRepository.save(offenderEntity().build())).thenReturn(offenderEntity().offenderUalId(OFFENDER_ENTITY_ID).build());

        final Optional<List<Long>> result = reportService.updateReport(mockReport);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(OFFENDER_ENTITY_ID);
        verify(offenderUalRepository).deleteAll();
    }

    @Test
    void replaceReportWithCro() {
        final var ualOffender = ualOffender().croPnc(OFFENDER_CRO).build();
        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        mockUsername();
        when(reportUtility.parseFromUalReport(any())).thenReturn(List.of(ualOffender));
        when(offenderUalRepository.save(offenderEntity()
            .offenderPnc(null)
            .offenderCro(FORMATTED_OFFENDER_CRO)
            .build()))
            .thenReturn(offenderEntity().offenderUalId(OFFENDER_ENTITY_ID).offenderPnc(null).offenderCro(FORMATTED_OFFENDER_CRO).build());

        final Optional<List<Long>> result = reportService.updateReport(mockReport);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(OFFENDER_ENTITY_ID);
        verify(offenderUalRepository).deleteAll();
    }

    @Test
    void replaceReportWhenFileParsesToEmpty() {
        when(reportUtility.parseFromUalReport(any())).thenReturn(emptyList());

        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        assertThat(reportService.updateReport(mockReport)).isEmpty();
    }


    private void mockUsername() {
        when(userSecurityUtils.getCurrentUsername()).thenReturn(Optional.of(USERNAME));
    }

    private UalOffenderBuilder ualOffender() {
        return UalOffender
            .builder()
            .nomsId("A1234AA")
            .prisonNumber("AW3222")
            .croPnc(OFFENDER_PNC)
            .firstNames("Tom")
            .familyName("Smith")
            .indexOffenceDescription("CONSPIRACY TO DEFRAUD");
    }

    private UalOffenderResponseBuilder expectedUalOffenderResponse() {
        return UalOffenderResponse
            .builder()
            .nomsId("A1234AA")
            .prisonNumber("AW3222")
            .pnc(FORMATTED_OFFENDER_PNC)
            .firstNames("Tom")
            .familyName("Smith")
            .indexOffenceDescription("CONSPIRACY TO DEFRAUD");
    }

    private OffenderUalEntityBuilder offenderEntity() {
        return OffenderUalEntity.builder()
            .offenderNo("A1234AA")
            .offenderBookingNo("AW3222")
            .offenderPnc(FORMATTED_OFFENDER_PNC)
            .firstNames("Tom")
            .lastName("Smith")
            .offenceDescription("CONSPIRACY TO DEFRAUD")
            .userId(USERNAME)
            .uploadDateTime(NOW);
    }
}
