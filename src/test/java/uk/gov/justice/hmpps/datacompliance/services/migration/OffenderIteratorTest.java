package uk.gov.justice.hmpps.datacompliance.services.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient.OffenderNumbersResponse;
import uk.gov.justice.hmpps.datacompliance.services.migration.OffenderIterator.OffenderAction;

import java.util.*;
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

    @Mock
    private Elite2ApiClient client;

    private OffenderIterator offenderIterator;

    @BeforeEach
    void setUp() {
        offenderIterator = new OffenderIterator(client, DataComplianceProperties.builder()
                .elite2ApiBaseUrl("some-url")
                .elite2ApiOffenderIdsIterationThreads(2)
                .elite2ApiOffenderIdsLimit(REQUEST_LIMIT)
                .elite2ApiOffenderIdsInitialOffset(0L)
                .build());
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
    void exceptionPreventsFurtherBatchesProcessing() {

        mockOffenderNumbersResponse("offender1", "offender2", "offender3");

        final var processedOffenderNumbers = new ArrayList<String>();
        final var processedFirst = new AtomicBoolean(false);
        final OffenderAction firstActionFails = offenderNumber -> {
            if (!processedFirst.getAndSet(true)) {
                throw new RuntimeException("First action fails!");
            }
            processedOffenderNumbers.add(offenderNumber.getOffenderNumber());
        };

        assertThatThrownBy(() -> offenderIterator.applyForAll(firstActionFails))
                .hasMessageContaining("First action fails!");
        assertThat(processedOffenderNumbers).doesNotContain("offender3");
    }

    @Test
    void canLimitIterationOverAConfigurableSubset() {

        offenderIterator = new OffenderIterator(client, new DataComplianceProperties("some-url", 2, REQUEST_LIMIT, 1L, 1L));

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
}