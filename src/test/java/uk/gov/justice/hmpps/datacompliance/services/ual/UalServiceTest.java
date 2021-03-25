package uk.gov.justice.hmpps.datacompliance.services.ual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity.OffenderUalEntityBuilder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.utils.ReportUtility;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse;

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
class UalServiceTest {

    public static final long OFFENDER_ENTITY_ID = 1L;
    private static final String USERNAME = "user1";
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Mock
    OffenderUalRepository offenderUalRepository;

    @Mock
    ReportUtility reportUtility;

    @Mock
    private UserSecurityUtils userSecurityUtils;

    private UalService ualService;

    @BeforeEach
    public void setUp(){
        ualService = new UalService(reportUtility, offenderUalRepository, TimeSource.of(NOW), userSecurityUtils);
    }

    @Test
    void getUalOffenders() {
        when(offenderUalRepository.findAll()).thenReturn(List.of(offenderEntity()));

        assertThat(ualService.getUalOffenders()).containsExactly(expectedUalOffenderResponse());
    }

    @Test
    void getUalOffendersWhenEmpty() {
        when(offenderUalRepository.findAll()).thenReturn(emptyList());

        assertThat(ualService.getUalOffenders()).isEmpty();
    }

    @Test
    void updateReport() {
        final var ualOffender = ualOffender();
        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        mockUsername();
        when(reportUtility.parseFromUalReport(any())).thenReturn(List.of(ualOffender));
        when(offenderUalRepository.findOneByOffenderNo(ualOffender.getNomsId())).thenReturn(Optional.of(offenderEntity(OFFENDER_ENTITY_ID)));
        when(offenderUalRepository.save(offenderEntity(OFFENDER_ENTITY_ID))).thenReturn(offenderEntity(OFFENDER_ENTITY_ID));

        final Optional<List<Long>> result = ualService.updateReport(mockReport);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(OFFENDER_ENTITY_ID);
        verify(offenderUalRepository).deleteByOffenderUalIdNotIn(List.of(OFFENDER_ENTITY_ID));
    }

    @Test
    void updateReportWhenNoNomsId() {
        final var offenderWithNoNomisId = UalOffender
            .builder()
            .prisonNumber("AW3222")
            .croPnc("569151/08")
            .firstNames("Tom")
            .familyName("Smith")
            .indexOffenceDescription("CONSPIRACY TO DEFRAUD")
            .build();

        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        mockUsername();
        when(reportUtility.parseFromUalReport(any())).thenReturn(List.of(offenderWithNoNomisId));
        when(offenderUalRepository.findOneByOffenderBookingNoAndOffenderCroPncAndFirstNamesAndLastName("AW3222", "569151/08", "Tom", "Smith")).thenReturn(Optional.of(offenderEntity(OFFENDER_ENTITY_ID)));
        when(offenderUalRepository.save(offenderEntityWithOutOffenderNo(OFFENDER_ENTITY_ID))).thenReturn(offenderEntityWithOutOffenderNo(OFFENDER_ENTITY_ID));

        final Optional<List<Long>> result = ualService.updateReport(mockReport);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(OFFENDER_ENTITY_ID);
        verify(offenderUalRepository).deleteByOffenderUalIdNotIn(List.of(OFFENDER_ENTITY_ID));
    }

    @Test
    void updateReportWhenNoExistingOffender() {
        final var ualOffender = ualOffender();
        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        mockUsername();
        when(reportUtility.parseFromUalReport(any())).thenReturn(List.of(ualOffender));
        when(offenderUalRepository.findOneByOffenderNo(ualOffender.getNomsId())).thenReturn(Optional.empty());
        when(offenderUalRepository.save(offenderEntity())).thenReturn(offenderEntity(OFFENDER_ENTITY_ID));

        final Optional<List<Long>> result = ualService.updateReport(mockReport);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(OFFENDER_ENTITY_ID);
        verify(offenderUalRepository).deleteByOffenderUalIdNotIn(List.of(OFFENDER_ENTITY_ID));
    }

    @Test
    void updateReportWhenNoExistingOffenderAndNoNomsNumber() {
        final var offenderWithNoNomisId = UalOffender
            .builder()
            .prisonNumber("AW3222")
            .croPnc("569151/08")
            .firstNames("Tom")
            .familyName("Smith")
            .indexOffenceDescription("CONSPIRACY TO DEFRAUD")
            .build();

        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        mockUsername();
        when(reportUtility.parseFromUalReport(any())).thenReturn(List.of(offenderWithNoNomisId));
        when(offenderUalRepository.findOneByOffenderBookingNoAndOffenderCroPncAndFirstNamesAndLastName("AW3222", "569151/08", "Tom", "Smith")).thenReturn(Optional.empty());
        when(offenderUalRepository.save(offenderEntityWithOutOffenderNo(null))).thenReturn(offenderEntityWithOutOffenderNo(OFFENDER_ENTITY_ID));

        final Optional<List<Long>> result = ualService.updateReport(mockReport);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(OFFENDER_ENTITY_ID);
        verify(offenderUalRepository).deleteByOffenderUalIdNotIn(List.of(OFFENDER_ENTITY_ID));
    }

    @Test
    void updateReportWhenFileParsesToEmpty() {
        when(reportUtility.parseFromUalReport(any())).thenReturn(emptyList());

        final var mockReport = new MockMultipartFile("someFile.csv", "someBytes".getBytes(StandardCharsets.UTF_8));

        assertThat(ualService.updateReport(mockReport)).isEmpty();
    }

    private void mockUsername() {
        when(userSecurityUtils.getCurrentUsername()).thenReturn(Optional.of(USERNAME));
    }

    private OffenderUalEntity offenderEntity(long id) {
        return offenderUalEntityBuilder().offenderUalId(id).build();
    }

    private OffenderUalEntity offenderEntity() {
        return offenderUalEntityBuilder().build();
    }

    private OffenderUalEntity offenderEntityWithOutOffenderNo(Long id) {
        return offenderUalEntityBuilder().offenderUalId(id).offenderNo(null).build();
    }

    private UalOffender ualOffender(){
            return UalOffender
                .builder()
                .nomsId("A1234AA")
                .prisonNumber("AW3222")
                .croPnc("569151/08")
                .firstNames("Tom")
                .familyName("Smith")
                .indexOffenceDescription("CONSPIRACY TO DEFRAUD")
                .build();
    }

    private UalOffenderResponse expectedUalOffenderResponse(){
        return UalOffenderResponse
            .builder()
            .nomsId("A1234AA")
            .prisonNumber("AW3222")
            .croPnc("569151/08")
            .firstNames("Tom")
            .familyName("Smith")
            .indexOffenceDescription("CONSPIRACY TO DEFRAUD")
            .build();
    }

    private OffenderUalEntityBuilder offenderUalEntityBuilder() {
        return  OffenderUalEntity.builder()
            .offenderNo("A1234AA")
            .offenderBookingNo("AW3222")
            .offenderCroPnc("569151/08")
            .firstNames("Tom")
            .lastName("Smith")
            .offenceDescription("CONSPIRACY TO DEFRAUD")
            .userId(USERNAME)
            .uploadDateTime(NOW);
    }
}
