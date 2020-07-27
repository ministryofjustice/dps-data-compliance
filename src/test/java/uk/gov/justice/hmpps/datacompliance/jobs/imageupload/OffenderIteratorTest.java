package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient.OffenderNumbersResponse;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.jobs.imageupload.OffenderIterator.OffenderAction;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageUploadBatchRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderIteratorTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.now();
    private static final long BATCH_ID = 1;
    private static final ImageUploadBatch BATCH = ImageUploadBatch.builder().batchId(BATCH_ID).build();
    private static final String OFFENDER_1 = "A1234AA";
    private static final String OFFENDER_2 = "B1234BB";
    private static final String OFFENDER_3 = "C1234CC";
    private static final String OFFENDER_4 = "D1234DD";
    private static final int REQUEST_LIMIT = 2;
    private static final DataComplianceProperties PROPERTIES = DataComplianceProperties.builder()
            .prisonApiBaseUrl("some-url")
            .prisonApiOffenderIdsIterationThreads(2)
            .prisonApiOffenderIdsLimit(REQUEST_LIMIT)
            .prisonApiOffenderIdsInitialOffset(0L)
            .build();

    @Mock
    private PrisonApiClient client;

    @Mock
    private ImageUploadBatchRepository repository;

    private OffenderIterator offenderIterator;

    @BeforeEach
    void setUp() {
        offenderIterator = new OffenderIterator(repository, client, PROPERTIES);
    }

    @Test
    void applyForAllHandlesEmptyListOfOffenders() {
        mockOffenderNumbersResponse(/* EMPTY */);
        assertThat(processedOffenderNumbers()).isEmpty();
    }

    @Test
    void applyForAllHandlesCountLessThanRequestLimit() {
        mockOffenderNumbersResponse(OFFENDER_1);
        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder(OFFENDER_1);
    }

    @Test
    void applyForAllHandlesCountEqualToRequestLimit() {
        mockOffenderNumbersResponse(OFFENDER_1, OFFENDER_2);
        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder(OFFENDER_1, OFFENDER_2);
    }

    @Test
    void applyForAllHandlesCountGreaterThanRequestLimit() {
        mockOffenderNumbersResponse(OFFENDER_1, OFFENDER_2, OFFENDER_3);
        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder(OFFENDER_1, OFFENDER_2, OFFENDER_3);
    }

    @Test
    void applyForAllWithRetry() {

        offenderIterator = new OffenderIterator(repository, client, PROPERTIES,
                RetryConfig.custom().maxAttempts(2)
                        .waitDuration(Duration.ZERO)
                        .build());

        mockOffenderNumbersResponse(OFFENDER_1, OFFENDER_2, OFFENDER_3);

        final var processedOffenderNumbers = new ArrayList<String>();

        offenderIterator.applyForAll(BATCH,
                throwOnFirstAttempt(offenderNumber -> processedOffenderNumbers.add(offenderNumber.getOffenderNumber())));

        assertThat(processedOffenderNumbers).containsExactlyInAnyOrder(OFFENDER_1, OFFENDER_2, OFFENDER_3);
    }

    @Test
    void exhaustedRetriesThrowsAndPreventsFurtherBatchesProcessing() {

        offenderIterator = new OffenderIterator(repository, client, PROPERTIES,
                RetryConfig.custom().maxAttempts(1).build());

        mockOffenderNumbersResponse(OFFENDER_1, OFFENDER_2, OFFENDER_3);

        final var processedOffenderNumbers = new ArrayList<String>();
        assertThatThrownBy(() -> offenderIterator.applyForAll(BATCH,
                throwOnFirstAttempt(offenderNumber -> processedOffenderNumbers.add(offenderNumber.getOffenderNumber()))))
                .hasMessageContaining("Failed!");

        assertThat(processedOffenderNumbers).doesNotContain(OFFENDER_3);
    }

    @Test
    void canLimitIterationOverAConfigurableSubset() {

        offenderIterator = new OffenderIterator(repository, client,
                DataComplianceProperties.builder()
                        .prisonApiOffenderIdsIterationThreads(2)
                        .prisonApiOffenderIdsLimit(REQUEST_LIMIT)
                        .prisonApiOffenderIdsInitialOffset(1L)
                        .prisonApiOffenderIdsTotalPages(1L)
                        .build());

        mockOffenderNumbersResponseWithOffset(1, OFFENDER_1, OFFENDER_2, OFFENDER_3, OFFENDER_4);

        assertThat(processedOffenderNumbers()).containsOnly(OFFENDER_2, OFFENDER_3);

    }

    @Test
    void applyForAllUsesAlternativeApiForUpdate() {

        when(repository.findFirstByBatchIdNotOrderByUploadStartDateTimeDesc(BATCH_ID))
                .thenReturn(Optional.of(ImageUploadBatch.builder().uploadStartDateTime(TIMESTAMP).build()));

        when(client.getOffendersWithNewImages(TIMESTAMP.toLocalDate(), 0, REQUEST_LIMIT)).thenReturn(response(3, List.of(OFFENDER_1, OFFENDER_2)));
        when(client.getOffendersWithNewImages(TIMESTAMP.toLocalDate(), 1, REQUEST_LIMIT)).thenReturn(response(3, List.of(OFFENDER_3)));

        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder(OFFENDER_1, OFFENDER_2, OFFENDER_3);
    }

    private List<String> processedOffenderNumbers() {
        final var processedOffenderNumbers = synchronizedList(new ArrayList<String>());
        offenderIterator.applyForAll(BATCH, offenderNumber -> processedOffenderNumbers.add(offenderNumber.getOffenderNumber()));
        return processedOffenderNumbers;
    }

    private void mockOffenderNumbersResponse(final String ... offenderNumbers) {
        mockOffenderNumbersResponseWithOffset(0, offenderNumbers);
    }

    private void mockOffenderNumbersResponseWithOffset(final int offset, final String ... offenderNumbers) {

        final var list = asList(offenderNumbers);
        final var count = new AtomicInteger(offset);

        when(repository.findFirstByBatchIdNotOrderByUploadStartDateTimeDesc(BATCH_ID)).thenReturn(Optional.empty());

        do {
            lenient().when(client.getOffenderNumbers(count.get(), REQUEST_LIMIT)).thenReturn(
                    response(list.size(), list.subList(count.get(), min(count.get() + REQUEST_LIMIT, list.size()))));

            count.addAndGet(REQUEST_LIMIT);
        } while (count.get() < list.size());
    }

    private OffenderNumbersResponse response(final long total, final Collection<String> offenderNumbers) {
        return OffenderNumbersResponse.builder()
                .totalCount(total)
                .offenderNumbers(offenderNumbers.stream().map(OffenderNumber::new).collect(toSet()))
                .build();
    }

    private OffenderAction throwOnFirstAttempt(final OffenderAction action) {

        final var failedAttempt = new AtomicBoolean();

        return offenderNumber -> {
            if (failedAttempt.get()) {
                action.accept(offenderNumber);
            } else {
                failedAttempt.set(true);
                throw new RuntimeException("Failed!");
            }
        };
    }
}