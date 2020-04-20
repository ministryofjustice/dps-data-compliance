package uk.gov.justice.hmpps.datacompliance.jobs.offenderdeletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.config.OffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderDeletionTest {

    private static final long BATCH_ID = 123L;
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime INITIAL_WINDOW_START = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
    private static final Duration DURATION = Duration.ofDays(1);

    private static final OffenderDeletionConfig CONFIG = OffenderDeletionConfig.builder()
            .initialWindowStart(INITIAL_WINDOW_START)
            .windowLength(DURATION)
            .build();

    @Mock
    private Elite2ApiClient elite2ApiClient;

    @Mock
    private OffenderDeletionBatchRepository batchRepository;

    private OffenderDeletion offenderDeletion;

    @BeforeEach
    void setUp() {
        offenderDeletion = new OffenderDeletion(TimeSource.of(NOW), CONFIG, batchRepository, elite2ApiClient);
    }

    @Test
    void sendInitialDeletionRequest() {

        final var expectedBatch = batchWith(INITIAL_WINDOW_START);

        when(batchRepository.findFirstByOrderByRequestDateTimeDesc()).thenReturn(Optional.empty());
        when(batchRepository.save(expectedBatch)).thenReturn(expectedBatch.withBatchId(BATCH_ID));

        offenderDeletion.run();

        verify(elite2ApiClient).requestPendingDeletions(INITIAL_WINDOW_START, INITIAL_WINDOW_START.plus(DURATION), BATCH_ID);
    }

    @Test
    void sendSubsequentDeletionRequest() {

        when(batchRepository.findFirstByOrderByRequestDateTimeDesc()).thenReturn(Optional.of(
                batchWith(INITIAL_WINDOW_START)));

        final var expectedBatch = batchWith(INITIAL_WINDOW_START.plus(DURATION));

        when(batchRepository.save(expectedBatch)).thenReturn(expectedBatch.withBatchId(BATCH_ID));

        offenderDeletion.run();

        verify(elite2ApiClient).requestPendingDeletions(INITIAL_WINDOW_START.plusDays(1), INITIAL_WINDOW_START.plusDays(2), BATCH_ID);
    }

    private OffenderDeletionBatch batchWith(final LocalDateTime windowStart) {
        return OffenderDeletionBatch.builder()
                .requestDateTime(NOW)
                .windowStartDateTime(windowStart)
                .windowEndDateTime(windowStart.plus(DURATION))
                .build();
    }
}
