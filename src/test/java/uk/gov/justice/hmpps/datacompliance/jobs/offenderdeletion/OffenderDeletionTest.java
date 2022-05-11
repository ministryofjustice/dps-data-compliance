package uk.gov.justice.hmpps.datacompliance.jobs.offenderdeletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.config.OffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionReferralRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;

@ExtendWith(MockitoExtension.class)
class OffenderDeletionTest {

    public static final String PROVISIONAL_DELETION_GRANTED = "PROVISIONAL_DELETION_GRANTED";
    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final long REFERRAL_ID = 123;
    private static final long BATCH_ID = 123L;
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    private static final LocalDate INITIAL_WINDOW_START = LocalDate.of(2020, 1, 2);
    private static final Duration DURATION = Duration.ofDays(1);
    private static final int NONE_REMAINING_IN_WINDOW = 0;
    private static final int SOME_REMAINING_IN_WINDOW = 1;
    private static final int REFERRAL_LIMIT = 1;
    private static final int DELETION_LIMIT = 1;
    private static final OffenderDeletionConfig CONFIG = OffenderDeletionConfig.builder()
        .initialWindowStart(INITIAL_WINDOW_START.atStartOfDay())
        .windowLength(DURATION)
        .referralLimit(REFERRAL_LIMIT)
        .reviewDuration(DURATION)
        .deletionLimit(DELETION_LIMIT)
        .build();


    @Mock
    private DataComplianceEventPusher eventPusher;

    @Mock
    private OffenderDeletionBatchRepository batchRepository;

    @Mock
    private OffenderDeletionReferralRepository offenderDeletionReferralRepository;

    @Mock
    private DataComplianceProperties properties;

    private OffenderDeletion offenderDeletion;

    @BeforeEach
    void setUp() {
        offenderDeletion = new OffenderDeletion(TimeSource.of(NOW), CONFIG, batchRepository, eventPusher, properties, offenderDeletionReferralRepository);
    }

    @Test
    void sendInitialDeletionRequest() {

        final var expectedBatch = batchWith(INITIAL_WINDOW_START.atStartOfDay());

        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.empty());
        when(batchRepository.save(expectedBatch)).thenReturn(expectedBatch.withBatchId(BATCH_ID));

        offenderDeletion.run();

        verify(eventPusher).requestReferral(OffenderDeletionReferralRequest.builder()
            .dueForDeletionWindowStart(INITIAL_WINDOW_START)
            .dueForDeletionWindowEnd(INITIAL_WINDOW_START.plusDays(DURATION.toDays()))
            .batchId(BATCH_ID)
            .limit(REFERRAL_LIMIT)
            .build());
    }

    @Test
    void sendSubsequentDeletionRequest() {

        final var expectedBatch = batchWith(INITIAL_WINDOW_START.plusDays(DURATION.toDays()).atStartOfDay());

        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.of(
            completedBatchWith(INITIAL_WINDOW_START.atStartOfDay(), NONE_REMAINING_IN_WINDOW)));
        when(batchRepository.save(expectedBatch)).thenReturn(expectedBatch.withBatchId(BATCH_ID));

        offenderDeletion.run();

        verify(eventPusher).requestReferral(OffenderDeletionReferralRequest.builder()
            .dueForDeletionWindowStart(INITIAL_WINDOW_START.plusDays(1))
            .dueForDeletionWindowEnd(INITIAL_WINDOW_START.plusDays(2))
            .batchId(BATCH_ID)
            .limit(REFERRAL_LIMIT)
            .build());
    }

    @Test
    void useSameWindowForNextBatchIfRemainingOffendersInWindow() {

        final var expectedBatch = batchWith(INITIAL_WINDOW_START.atStartOfDay());

        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.of(
            completedBatchWith(INITIAL_WINDOW_START.atStartOfDay(), SOME_REMAINING_IN_WINDOW)));
        when(batchRepository.save(expectedBatch)).thenReturn(expectedBatch.withBatchId(BATCH_ID));

        offenderDeletion.run();

        verify(eventPusher).requestReferral(OffenderDeletionReferralRequest.builder()
            .dueForDeletionWindowStart(INITIAL_WINDOW_START)
            .dueForDeletionWindowEnd(INITIAL_WINDOW_START.plusDays(DURATION.toDays()))
            .batchId(BATCH_ID)
            .limit(REFERRAL_LIMIT)
            .build());
    }

    @Test
    void offenderDeletionRequestFailsIfLastBatchDidNotComplete() {

        final var incompleteBatch = batchWith(INITIAL_WINDOW_START.atStartOfDay());
        incompleteBatch.setBatchId(BATCH_ID);

        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.of(incompleteBatch));

        assertThatThrownBy(() -> offenderDeletion.run())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Previous referral (123) did not complete");
    }

    @Test
    void offenderDeletionRequestFailsIfStartDateInFuture() {

        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.of(
            completedBatchWith(NOW.plusSeconds(1), NONE_REMAINING_IN_WINDOW)));

        assertThatThrownBy(() -> offenderDeletion.run())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deletion due date cannot be in the future, window start date is not valid");
    }

    @Test
    void offenderDeletionRequestFailsIfEndDateInFuture() {

        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.of(
            completedBatchWith(NOW.minusDays(2).plusSeconds(1), NONE_REMAINING_IN_WINDOW)));

        assertThatThrownBy(() -> offenderDeletion.run())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deletion due date cannot be in the future, window end date is not valid");
    }

    @Test
    void offenderDeletionRequestFailsIfWindowDatesIllogical() {

        final var badConfig = OffenderDeletionConfig.builder()
            .initialWindowStart(INITIAL_WINDOW_START.atStartOfDay())
            .windowLength(Duration.ofDays(-1))
            .build();

        offenderDeletion = new OffenderDeletion(TimeSource.of(NOW), badConfig, batchRepository, eventPusher, properties, offenderDeletionReferralRepository);
        when(batchRepository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offenderDeletion.run())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Deletion due window dates are illogical");
    }

    @Test
    void deletePreviouslyIdentifiedOffenderData() {
        final var offenderDeletionReferral = buildOffenderDeletionReferral();
        final var expectedDate = NOW.minus(CONFIG.getReviewDuration());

        when(properties.isDeletionGrantEnabled()).thenReturn(true);
        when(properties.isReviewRequired()).thenReturn(true);
        when(offenderDeletionReferralRepository.findByReferralResolutionStatus(PROVISIONAL_DELETION_GRANTED, true, expectedDate, DELETION_LIMIT))
            .thenReturn(List.of(offenderDeletionReferral));

        offenderDeletion.deletePreviouslyIdentifiedOffenderData();

        verify(eventPusher).requestProvisionalDeletionReferral(new OffenderNumber(OFFENDER_NUMBER), REFERRAL_ID);
    }

    @Test
    void deletePreviouslyIdentifiedOffenderDataWhenNoReferralsFound() {
        final var expectedDate = NOW.minus(CONFIG.getReviewDuration());

        when(properties.isDeletionGrantEnabled()).thenReturn(true);
        when(properties.isReviewRequired()).thenReturn(true);
        when(offenderDeletionReferralRepository.findByReferralResolutionStatus(PROVISIONAL_DELETION_GRANTED, true, expectedDate, DELETION_LIMIT))
            .thenReturn(emptyList());

        offenderDeletion.deletePreviouslyIdentifiedOffenderData();

        verifyNoInteractions(eventPusher);
    }


    @ParameterizedTest()
    @CsvSource({"true,false", "false,true"})
    void deletePreviouslyIdentifiedOffenderDataDoesNothingWhenNotConfigured(final boolean isDeletionGranted, final boolean isReviewRequired) {
        when(properties.isDeletionGrantEnabled()).thenReturn(isDeletionGranted);
        lenient().when(properties.isReviewRequired()).thenReturn(isReviewRequired);

        offenderDeletion.deletePreviouslyIdentifiedOffenderData();

        verifyNoInteractions(eventPusher);
    }


    private OffenderDeletionBatch batchWith(final LocalDateTime windowStart) {
        return OffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .windowStartDateTime(windowStart)
            .windowEndDateTime(windowStart.plus(DURATION))
            .batchType(SCHEDULED)
            .build();
    }

    private OffenderDeletionBatch completedBatchWith(final LocalDateTime windowStart, final int remainingInWindow) {
        final var batch = batchWith(windowStart);
        batch.setRemainingInWindow(remainingInWindow);
        batch.setReferralCompletionDateTime(NOW);
        return batch;
    }

    private OffenderDeletionReferral buildOffenderDeletionReferral() {
        return OffenderDeletionReferral.builder()
            .referralId(REFERRAL_ID)
            .offenderNo(OFFENDER_NUMBER)
            .offenderDeletionBatch(completedBatchWith(NOW.plusSeconds(1), NONE_REMAINING_IN_WINDOW))
            .build();
    }
}
