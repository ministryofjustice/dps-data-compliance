package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient.OffenderNumbersResponse;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.LongStream;

import static io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff;
import static io.github.resilience4j.retry.RetryConfig.custom;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.callable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.propagateAnyError;

@Slf4j
@Service
class OffenderIterator {

    private static final int UPLOAD_RETRY_MAX_ATTEMPTS = 5;
    private static final Duration UPLOAD_RETRY_INITIAL_INTERVAL = Duration.ofMillis(500);
    private static final double UPLOAD_RETRY_MULTIPLIER = 2;

    private final PrisonApiClient prisonApiClient;
    private final DataComplianceProperties properties;
    private final ExecutorService executorService;
    private final RetryConfig retryConfig;

    @Autowired
    public OffenderIterator(final PrisonApiClient prisonApiClient,
                            final DataComplianceProperties properties) {
        this(prisonApiClient, properties, custom()
                .maxAttempts(UPLOAD_RETRY_MAX_ATTEMPTS)
                .intervalFunction(ofExponentialBackoff(UPLOAD_RETRY_INITIAL_INTERVAL, UPLOAD_RETRY_MULTIPLIER))
                .build());
    }

    @VisibleForTesting
    OffenderIterator(final PrisonApiClient prisonApiClient,
                     final DataComplianceProperties properties,
                     final RetryConfig retryConfig) {
        this.executorService = newFixedThreadPool(properties.getPrisonApiOffenderIdsIterationThreads());
        this.prisonApiClient = prisonApiClient;
        this.properties = properties;
        this.retryConfig = retryConfig;
    }

    void applyForAll(final OffenderAction action) {

        log.info("Applying offender action to first batch of up to {} offenders, offset: {}",
                pageLimit(), properties.getPrisonApiOffenderIdsInitialOffset());
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

        final var offset = properties.getPrisonApiOffenderIdsInitialOffset() + (batchIndex * pageLimit());
        final var response = prisonApiClient.getOffenderNumbers(offset, pageLimit());
        final var tasks = response.getOffenderNumbers().stream()
                .map(offenderNumber -> applyWithRetry(action, offenderNumber))
                .collect(toList());

        try {

            executorService.invokeAll(tasks)
                    .forEach(future -> propagateAnyError(future::get));

        } catch (InterruptedException e) {

            currentThread().interrupt();

            throw new IllegalStateException("Execution of batch interrupted", e);
        }

        return response;
    }

    private long indexOfFinalBatch(final OffenderNumbersResponse response) {

        final var totalBatches = (long) ceil((double) response.getTotalCount() / pageLimit());

        return min(totalBatches, properties.getOffenderIdsTotalPages().orElse(MAX_VALUE)) - 1;
    }

    private long pageLimit() {
        return properties.getPrisonApiOffenderIdsLimit();
    }

    interface OffenderAction extends Consumer<OffenderNumber> { }

    private Callable<Object> applyWithRetry(final OffenderAction offenderAction, final OffenderNumber offenderNumber) {
        return callable(() -> Retry.of(offenderNumber.getOffenderNumber(), retryConfig)
                .executeRunnable(() -> offenderAction.accept(offenderNumber)));
    }
}
