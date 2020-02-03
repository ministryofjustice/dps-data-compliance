package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient.OffenderNumbersResponse;

import java.util.function.Consumer;
import java.util.stream.LongStream;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.ceil;
import static java.lang.Math.min;

@Slf4j
@Service
@AllArgsConstructor
class OffenderIterator {

    private final Elite2ApiClient elite2ApiClient;
    private final DataComplianceProperties properties;

    void applyForAll(final OffenderAction action) {

        log.info("Applying offender action to first batch of up to {} offenders, offset: {}",
                pageLimit(), properties.getElite2ApiOffenderIdsInitialOffset());
        final var firstBatchResponse = applyForBatch(action, 0);

        log.info("Total number of {} offenders", firstBatchResponse.getTotalCount());
        properties.getOffenderIdsTotalPages()
                .ifPresent(total -> log.info("Limiting iteration to {} pages of data", total));

        LongStream.rangeClosed(1, indexOfFinalBatch(firstBatchResponse))
                .forEach(batchIndex -> applyForBatch(action, batchIndex));

        log.info("Offender action applied");
    }

    private OffenderNumbersResponse applyForBatch(final OffenderAction action, final long batchIndex) {

        log.info("Applying offender action to batch {}", batchIndex);

        final var offset = properties.getElite2ApiOffenderIdsInitialOffset() + (batchIndex * pageLimit());
        final var response = elite2ApiClient.getOffenderNumbers(offset, pageLimit());

        response.getOffenderNumbers().forEach(action);

        return response;
    }

    private long indexOfFinalBatch(final OffenderNumbersResponse response) {

        final var totalBatches = (long) ceil((double) response.getTotalCount() / pageLimit());

        return min(totalBatches, properties.getOffenderIdsTotalPages().orElse(MAX_VALUE)) - 1;
    }

    private long pageLimit() {
        return properties.getElite2ApiOffenderIdsLimit();
    }

    interface OffenderAction extends Consumer<OffenderNumber> { }

}
