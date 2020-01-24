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
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderIteratorTest {

    private static final int REQUEST_LIMIT = 2;

    @Mock
    private Elite2ApiClient client;

    private OffenderIterator offenderIterator;

    @BeforeEach
    void setUp() {
        offenderIterator = new OffenderIterator(client, new DataComplianceProperties("some-url", REQUEST_LIMIT));
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
    void exceptionPreventsFurtherProcessing() {

        mockOffenderNumbersResponse("offender1", "offender2");

        final var processedOffenderNumbers = new ArrayList<String>();
        final var processedFirst = new AtomicBoolean(false);
        final OffenderAction firstActionFails = offenderNumber -> {
            if (!processedFirst.getAndSet(true)) {
                throw new RuntimeException("First action fails!");
            }
            processedOffenderNumbers.add(offenderNumber.getOffenderNumber());
        };

        assertThatThrownBy(() -> offenderIterator.applyForAll(firstActionFails));
        assertThat(processedOffenderNumbers).isEmpty();
    }

    private List<String> processedOffenderNumbers() {
        final var processedOffenderNumbers = new ArrayList<String>();
        offenderIterator.applyForAll(offenderNumber ->
                processedOffenderNumbers.add(offenderNumber.getOffenderNumber()));
        return processedOffenderNumbers;
    }

    private void mockOffenderNumbersResponse(final String ... offenderNumbers) {

        final var list = stream(offenderNumbers).map(OffenderNumber::new).collect(toList());
        final var count = new AtomicInteger();

        do {
            when(client.getOffenderNumbers(count.get(), REQUEST_LIMIT))
                    .thenReturn(OffenderNumbersResponse.builder()
                            .totalCount(list.size())
                            .offenderNumbers(new HashSet<>(
                                    list.subList(count.get(), min(count.get() + REQUEST_LIMIT, list.size()))))
                            .build());

            count.addAndGet(REQUEST_LIMIT);
        } while (count.get() < list.size());
    }
}