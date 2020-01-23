package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient.OffenderNumbersResponse;

import java.util.function.Consumer;
import java.util.stream.LongStream;

import static java.lang.Math.ceil;

@Slf4j
@AllArgsConstructor
public class OffenderIterator {

    private final Elite2ApiClient elite2ApiClient;
    private final long requestLimit;

    public void applyForAll(final OffenderAction action) {

        log.info("Applying offender action to first batch of {} offenders", requestLimit);
        final var firstBatchResponse = applyForBatch(action, 0);

        log.info("Applying offender action to a total of {} offenders", firstBatchResponse.getTotalCount());
        LongStream.rangeClosed(1, indexOfFinalBatch(firstBatchResponse))
                .forEach(batchIndex -> applyForBatch(action, batchIndex));

        log.info("Offender action applied");
    }

    private OffenderNumbersResponse applyForBatch(final OffenderAction action, final long batchIndex) {

        log.info("Applying offender action to batch {}", batchIndex);

        final var response = elite2ApiClient.getOffenderNumbers(batchIndex * requestLimit, requestLimit);
        response.getOffenderNumbers().forEach(action);
        return response;
    }

    private long indexOfFinalBatch(final OffenderNumbersResponse response) {
        return (long) ceil((double) response.getTotalCount() / requestLimit) - 1;
    }

    public interface OffenderAction extends Consumer<OffenderNumber> { }

}
