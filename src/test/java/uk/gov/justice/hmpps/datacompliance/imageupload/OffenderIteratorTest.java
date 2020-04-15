package uk.gov.justice.hmpps.datacompliance.imageupload;

import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.Elite2ApiClient.OffenderNumbersResponse;
import uk.gov.justice.hmpps.datacompliance.imageupload.OffenderIterator.OffenderAction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OffenderIteratorTest {

    private static final int REQUEST_LIMIT = 2;
    private static final DataComplianceProperties PROPERTIES = DataComplianceProperties.builder()
            .elite2ApiBaseUrl("some-url")
            .elite2ApiOffenderIdsIterationThreads(2)
            .elite2ApiOffenderIdsLimit(REQUEST_LIMIT)
            .elite2ApiOffenderIdsInitialOffset(0L)
            .build();

    @Mock
    private Elite2ApiClient client;

    private OffenderIterator offenderIterator;

    @BeforeEach
    void setUp() {
        offenderIterator = new OffenderIterator(client, PROPERTIES);
    }

    @Test
    void applyForAllHandlesEmptyListOfOffenders() {
        mockOffenderNumbersResponse(/* EMPTY */);
        assertThat(processedOffenderNumbers()).isEmpty();
    }

    @Test
    void applyForAllHandlesCountLessThanRequestLimit() {
        mockOffenderNumbersResponse("offender1");
        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder("offender1");
    }

    @Test
    void applyForAllHandlesCountEqualToRequestLimit() {
        mockOffenderNumbersResponse("offender1", "offender2");
        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder("offender1", "offender2");
    }

    @Test
    void applyForAllHandlesCountGreaterThanRequestLimit() {
        mockOffenderNumbersResponse("offender1", "offender2", "offender3");
        assertThat(processedOffenderNumbers()).containsExactlyInAnyOrder("offender1", "offender2", "offender3");
    }

    @Test
    void applyForAllWithRetry() {

        offenderIterator = new OffenderIterator(client, PROPERTIES,
                RetryConfig.custom().maxAttempts(2)
                        .waitDuration(Duration.ZERO)
                        .build());

        mockOffenderNumbersResponse("offender1", "offender2", "offender3");

        final var processedOffenderNumbers = new ArrayList<String>();

        offenderIterator.applyForAll(throwOnFirstAttempt(
                offenderNumber -> processedOffenderNumbers.add(offenderNumber.getOffenderNumber())));

        assertThat(processedOffenderNumbers).containsExactlyInAnyOrder("offender1", "offender2", "offender3");
    }

    @Test
    void exhaustedRetriesThrowsAndPreventsFurtherBatchesProcessing() {

        offenderIterator = new OffenderIterator(client, PROPERTIES,
                RetryConfig.custom().maxAttempts(1).build());

        mockOffenderNumbersResponse("offender1", "offender2", "offender3");

        final var processedOffenderNumbers = new ArrayList<String>();
        assertThatThrownBy(() -> offenderIterator.applyForAll(throwOnFirstAttempt(
                offenderNumber -> processedOffenderNumbers.add(offenderNumber.getOffenderNumber()))))
                .hasMessageContaining("Failed!");

        assertThat(processedOffenderNumbers).doesNotContain("offender3");
    }

    @Test
    void canLimitIterationOverAConfigurableSubset() {

        offenderIterator = new OffenderIterator(client,
                DataComplianceProperties.builder()
                        .elite2ApiOffenderIdsIterationThreads(2)
                        .elite2ApiOffenderIdsLimit(REQUEST_LIMIT)
                        .elite2ApiOffenderIdsInitialOffset(1L)
                        .elite2ApiOffenderIdsTotalPages(1L)
                        .build());

        mockOffenderNumbersResponseWithOffset(1, "offender1", "offender2", "offender3", "offender4");

        assertThat(processedOffenderNumbers()).containsOnly("offender2", "offender3");

    }

    private List<String> processedOffenderNumbers() {
        final var processedOffenderNumbers = synchronizedList(new ArrayList<String>());
        offenderIterator.applyForAll(offenderNumber ->
                processedOffenderNumbers.add(offenderNumber.getOffenderNumber()));
        return processedOffenderNumbers;
    }

    private void mockOffenderNumbersResponse(final String ... offenderNumbers) {
        mockOffenderNumbersResponseWithOffset(0, offenderNumbers);
    }

    private void mockOffenderNumbersResponseWithOffset(final int offset, final String ... offenderNumbers) {

        final var list = asList(offenderNumbers);
        final var count = new AtomicInteger(offset);

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