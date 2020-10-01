package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.RetentionReasonCode;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.RetentionReasonCode.Code;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.ManualRetentionRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.RetentionReasonCodeRepository;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReason;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonDisplayName;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionRequest;

import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode.HIGH_PROFILE;
import static uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode.OTHER;

@ExtendWith(MockitoExtension.class)
class ManualRetentionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    private static final String OFFENDER_NO = "A1234BC";
    private static final String USERNAME = "user1";
    private static final String REASON_DETAILS = "Some reason";
    private static final ManualRetention EXISTING_RECORD = ManualRetention.builder().retentionVersion(0).build();

    @Mock
    private UserSecurityUtils userSecurityUtils;

    @Mock
    private ManualRetentionRepository manualRetentionRepository;

    @Mock
    private RetentionReasonCodeRepository retentionReasonCodeRepository;

    private ManualRetentionService manualRetentionService;

    @BeforeEach
    void setUp() {
        manualRetentionService = new ManualRetentionService(
                TimeSource.of(NOW),
                userSecurityUtils,
                manualRetentionRepository,
                retentionReasonCodeRepository);
    }

    @Test
    void getRetentionReasons() {

        when(retentionReasonCodeRepository.findAll()).thenReturn(List.of(
                RetentionReasonCode.builder()
                        .retentionReasonCodeId(Code.HIGH_PROFILE)
                        .displayName("High Profile Offenders")
                        .allowReasonDetails(false)
                        .displayOrder(1)
                        .build(),
                RetentionReasonCode.builder()
                        .retentionReasonCodeId(Code.OTHER)
                        .displayName("Other")
                        .allowReasonDetails(true)
                        .displayOrder(2)
                        .build()));

        assertThat(manualRetentionService.getRetentionReasons()).containsExactly(
                ManualRetentionReasonDisplayName.builder()
                        .reasonCode(HIGH_PROFILE)
                        .displayName("High Profile Offenders")
                        .allowReasonDetails(false)
                        .displayOrder(1)
                        .build(),
                ManualRetentionReasonDisplayName.builder()
                        .reasonCode(OTHER)
                        .displayName("Other")
                        .allowReasonDetails(true)
                        .displayOrder(2)
                        .build());
    }

    @Test
    void findManualOffenderRetention() {

        final var manualRetention = mock(ManualRetention.class);

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.of(manualRetention));

        assertThat(manualRetentionService.findManualOffenderRetention(new OffenderNumber(OFFENDER_NO)))
                .contains(manualRetention);
    }

    @Test
    void findManualOffenderRetentionReturnsEmpty() {

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.empty());

        assertThat(manualRetentionService.findManualOffenderRetention(new OffenderNumber(OFFENDER_NO)))
                .isEmpty();
    }

    @Test
    void findManualOffenderRetentionWithReasons() {

        final var manualRetention = ManualRetention.builder().build();

        manualRetention.addManualRetentionReason(
                uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetentionReason.builder().build());

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.of(manualRetention));

        assertThat(manualRetentionService.findManualOffenderRetentionWithReasons(new OffenderNumber(OFFENDER_NO)))
                .contains(manualRetention);
    }

    @Test
    void findManualOffenderRetentionWithReasonsReturnsEmptyWhenNoRetentionRecord() {

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.empty());

        assertThat(manualRetentionService.findManualOffenderRetentionWithReasons(new OffenderNumber(OFFENDER_NO)))
                .isEmpty();
    }

    @Test
    void findManualOffenderRetentionWithReasonsReturnsEmptyWhenReasonsAreEmpty() {

        final var manualRetention = ManualRetention.builder().build();

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.of(manualRetention));

        assertThat(manualRetentionService.findManualOffenderRetentionWithReasons(new OffenderNumber(OFFENDER_NO)))
                .isEmpty();
    }

    @Test
    void checkEnumsContainSameNames() {

        final var dbNames = stream(Code.values()).map(Enum::name).sorted().collect(toList());
        final var webNames = stream(ManualRetentionReasonCode.values()).map(Enum::name).sorted().collect(toList());

        assertThat(dbNames).containsExactlyElementsOf(webNames);
    }

    @Test
    void createManualOffenderRetention() {

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.empty());

        mockUsername();
        mockReasonCodeRetrieval(Code.HIGH_PROFILE);

        manualRetentionService.updateManualOffenderRetention(
                new OffenderNumber(OFFENDER_NO), requestWith(HIGH_PROFILE), null);

        assertEntityPersistedWith(0, Code.HIGH_PROFILE);
    }

    @Test
    void updateManualOffenderRetention() {

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.of(EXISTING_RECORD));

        mockUsername();
        mockReasonCodeRetrieval(Code.OTHER);

        manualRetentionService.updateManualOffenderRetention(
                new OffenderNumber(OFFENDER_NO), requestWith(OTHER), "\"0\"");

        assertEntityPersistedWith(1, Code.OTHER);
    }

    @Test
    void updateManualOffenderRetentionThrowsWhenIfMatchMissing() {

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.of(EXISTING_RECORD));

        assertThatThrownBy(() ->
                manualRetentionService.updateManualOffenderRetention(
                        new OffenderNumber(OFFENDER_NO), requestWith(OTHER), null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("Must provide 'If-Match' header");
    }

    @Test
    void updateManualOffenderRetentionThrowsWhenExistingVersionDoesNotMatch() {

        when(manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(OFFENDER_NO))
                .thenReturn(Optional.of(EXISTING_RECORD));

        assertThatThrownBy(() ->
                manualRetentionService.updateManualOffenderRetention(
                        new OffenderNumber(OFFENDER_NO), requestWith(OTHER), "\"-1\""))
                .isInstanceOf(OptimisticLockException.class)
                .hasMessageContaining("Attempting to update an old version of the retention record");
    }

    private void assertEntityPersistedWith(final int version, final Code reasonCode) {

        final var capturedEntity = ArgumentCaptor.forClass(ManualRetention.class);
        verify(manualRetentionRepository).save(capturedEntity.capture());

        final var persistedEntity = capturedEntity.getValue();
        assertThat(persistedEntity.getOffenderNo()).isEqualTo(OFFENDER_NO);
        assertThat(persistedEntity.getUserId()).isEqualTo(USERNAME);
        assertThat(persistedEntity.getRetentionVersion()).isEqualTo(version);
        assertThat(persistedEntity.getRetentionDateTime()).isEqualTo(NOW);

        assertThat(persistedEntity.getManualRetentionReasons()).hasSize(1);

        final var retentionReason = persistedEntity.getManualRetentionReasons().get(0);
        assertThat(retentionReason.getRetentionReasonCodeId().getRetentionReasonCodeId()).isEqualTo(reasonCode);
        assertThat(retentionReason.getReasonDetails()).isEqualTo("Some reason");
    }

    private ManualRetentionRequest requestWith(final ManualRetentionReasonCode code) {
        return ManualRetentionRequest.builder()
                .retentionReason(ManualRetentionReason.builder()
                        .reasonCode(code)
                        .reasonDetails(REASON_DETAILS)
                        .build())
                .build();
    }

    private void mockUsername() {
        when(userSecurityUtils.getCurrentUsername()).thenReturn(Optional.of(USERNAME));
    }

    private void mockReasonCodeRetrieval(final Code code) {

        when(retentionReasonCodeRepository.findById(code))
                .thenReturn(Optional.of(RetentionReasonCode.builder()
                        .retentionReasonCodeId(code)
                        .build()));
    }
}
