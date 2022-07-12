package uk.gov.justice.hmpps.datacompliance.services.deletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult.DeceasedOffender;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult.OffenderAlias;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch.BatchType;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.ReferredDeceasedOffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeceasedDeletionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final long OFFENDER_ID = 456;
    private static final long OFFENDER_BOOK_ID = 788;
    private static final long BATCH_ID = 1L;
    private static final LocalDateTime REFERRAL_COMPLETION_DATE_TIME = LocalDateTime.now().minusHours(2);

    @Mock
    private DeceasedOffenderDeletionReferralRepository referralRepository;

    @Mock
    private DataComplianceEventPusher deletionGrantedEventPusher;

    @Mock
    private ImageDuplicationDetectionService imageDuplicationDetectionService;

    @Mock
    private DeceasedOffenderDeletionBatchRepository batchRepository;

    @Mock
    private DeceasedOffenderDeletionBatch batch;

    private DeceasedDeletionService deceasedDeletionService;

    public static DeceasedOffender buildDeceasedOffender() {
        return DeceasedOffender.builder()
            .offenderIdDisplay(OFFENDER_NUMBER)
            .firstName("someFirstName")
            .middleName("someMiddleName")
            .lastName("someLastName")
            .agencyLocationId("someAgencyLocationId")
            .birthDate(LocalDate.now().minusYears(30))
            .deceasedDate(LocalDate.now().minusYears(1))
            .deletionDateTime(LocalDateTime.now().minusMinutes(1))
            .offenderAlias(buildOffenderAlias())
            .build();
    }

    private static OffenderAlias buildOffenderAlias() {
        return OffenderAlias.builder()
            .offenderId(OFFENDER_ID)
            .offenderBookId(OFFENDER_BOOK_ID)
            .build();
    }

    @BeforeEach
    void setUp() {
        deceasedDeletionService = new DeceasedDeletionService(
            TimeSource.of(NOW),
            batchRepository,
            referralRepository);
    }

    @Test
    void handleDeceasedOffenderDeletionResult() {

        final var deletionResult = buildDeceasedOffenderDeletionResult();
        var batch = buildOffenderDeletionBatch();
        final var updatedBatch = updateBatch(batch);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(updatedBatch)).thenReturn(updatedBatch);

        deceasedDeletionService.handleDeceasedOffenderDeletionResult(deletionResult);

        verify(referralRepository).save(expectedDeceasedOffenderDeletionReferral(updatedBatch, deletionResult));
    }

    @Test
    void handleDeletionCompleteThrowsIfBatchNotFound() {

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            deceasedDeletionService.handleDeceasedOffenderDeletionResult(DeceasedOffenderDeletionResult.builder().batchId(BATCH_ID).build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot find batch with id: '1'");

        verifyNoInteractions(referralRepository);
    }

    private DeceasedOffenderDeletionReferral expectedDeceasedOffenderDeletionReferral(DeceasedOffenderDeletionBatch updatedBatch, DeceasedOffenderDeletionResult deletionResult) {
        final var deceasedOffender = deletionResult.getDeceasedOffenders().get(0);
        final var offenderAlias = deceasedOffender.getOffenderAliases().get(0);

        final var referral = DeceasedOffenderDeletionReferral.builder()
            .deceasedOffenderDeletionBatch(updatedBatch)
            .offenderNo(deceasedOffender.getOffenderIdDisplay())
            .firstName(deceasedOffender.getFirstName())
            .middleName(deceasedOffender.getMiddleName())
            .lastName(deceasedOffender.getLastName())
            .birthDate(deceasedOffender.getBirthDate())
            .deceasedDate(deceasedOffender.getDeceasedDate())
            .agencyLocationId(deceasedOffender.getAgencyLocationId())
            .deletionDateTime(deceasedOffender.getDeletionDateTime())
            .build();


        referral.addReferredOffenderAlias(
            ReferredDeceasedOffenderAlias.builder()
                .offenderId(offenderAlias.getOffenderId())
                .offenderBookId(offenderAlias.getOffenderBookIds().get(0))
                .build());

        return referral;
    }

    private DeceasedOffenderDeletionBatch updateBatch(DeceasedOffenderDeletionBatch batch) {
        return batch.toBuilder()
            .referralCompletionDateTime(NOW)
            .build();
    }

    private DeceasedOffenderDeletionBatch buildOffenderDeletionBatch() {
        return DeceasedOffenderDeletionBatch.builder()
            .batchId(BATCH_ID)
            .batchType(BatchType.SCHEDULED)
            .requestDateTime(LocalDateTime.now().minusDays(1))
            .build();
    }

    private DeceasedOffenderDeletionResult buildDeceasedOffenderDeletionResult() {
        return DeceasedOffenderDeletionResult.builder()
            .batchId(BATCH_ID)
            .deceasedOffender(buildDeceasedOffender())
            .build();
    }

}