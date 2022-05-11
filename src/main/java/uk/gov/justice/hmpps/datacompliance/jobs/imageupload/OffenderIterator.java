package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient.OffenderNumbersResponse;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageUploadBatchRepository;

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

    private final ImageUploadBatchRepository repository;
    private final PrisonApiClient prisonApiClient;
    private final DataComplianceProperties properties;
    private final ExecutorService executorService;
    private final RetryConfig retryConfig;

    @Autowired
    public OffenderIterator(final ImageUploadBatchRepository repository,
                            final PrisonApiClient prisonApiClient,
                            final DataComplianceProperties properties) {
        this(repository, prisonApiClient, properties, custom()
            .maxAttempts(UPLOAD_RETRY_MAX_ATTEMPTS)
            .intervalFunction(ofExponentialBackoff(UPLOAD_RETRY_INITIAL_INTERVAL, UPLOAD_RETRY_MULTIPLIER))
            .build());
    }

    @VisibleForTesting
    OffenderIterator(final ImageUploadBatchRepository repository,
                     final PrisonApiClient prisonApiClient,
                     final DataComplianceProperties properties,
                     final RetryConfig retryConfig) {
        this.repository = repository;
        this.executorService = newFixedThreadPool(properties.getPrisonApiOffenderIdsIterationThreads());
        this.prisonApiClient = prisonApiClient;
        this.properties = properties;
        this.retryConfig = retryConfig;
    }

    void applyForAll(final ImageUploadBatch batch, final OffenderAction action) {

        log.info("Applying offender action to first page of up to {} offenders, offset: {}",
            pageLimit(), properties.getPrisonApiOffenderIdsInitialOffset());
        final var firstPageResponse = applyForPage(0, action, batch);

        log.info("Total number of {} offenders", firstPageResponse.getTotalCount());
        properties.getOffenderIdsTotalPages()
            .ifPresent(total -> log.info("Limiting iteration to {} pages of data", total));

        LongStream.rangeClosed(1, indexOfFinalPage(firstPageResponse))
            .forEach(pageNumber -> applyForPage(pageNumber, action, batch));

        log.info("Offender action applied");
    }

    private OffenderNumbersResponse applyForPage(final long pageNumber,
                                                 final OffenderAction action,
                                                 final ImageUploadBatch batch) {

        log.info("Applying offender action to page {}", pageNumber);

        final var offset = properties.getPrisonApiOffenderIdsInitialOffset() + (pageNumber * pageLimit());
        final var response = getOffenderNumbers(offset, pageLimit(), batch);
        final var tasks = response.getOffenderNumbers().stream()
            .map(offenderNumber -> applyWithRetry(action, offenderNumber))
            .collect(toList());

        try {

            executorService.invokeAll(tasks)
                .forEach(future -> propagateAnyError(future::get));

        } catch (InterruptedException e) {

            currentThread().interrupt();

            throw new IllegalStateException("Execution interrupted", e);
        }

        return response;
    }

    private OffenderNumbersResponse getOffenderNumbers(final long offset,
                                                       final long limit,
                                                       final ImageUploadBatch batch) {

        return repository.findFirstByBatchIdNotOrderByUploadStartDateTimeDesc(batch.getBatchId())
            .map(lastUpload -> prisonApiClient.getOffendersWithNewImages(
                lastUpload.getUploadStartDateTime().toLocalDate(), offset / limit, limit))
            .orElseGet(() -> prisonApiClient.getOffenderNumbers(offset, limit));
    }

    private long indexOfFinalPage(final OffenderNumbersResponse response) {

        final var totalBatches = (long) ceil((double) response.getTotalCount() / pageLimit());

        return min(totalBatches, properties.getOffenderIdsTotalPages().orElse(MAX_VALUE)) - 1;
    }

    private long pageLimit() {
        return properties.getPrisonApiOffenderIdsLimit();
    }

    private Callable<Object> applyWithRetry(final OffenderAction offenderAction, final OffenderNumber offenderNumber) {
        return callable(() -> Retry.of(offenderNumber.getOffenderNumber(), retryConfig)
            .executeRunnable(() -> offenderAction.accept(offenderNumber)));
    }

    interface OffenderAction extends Consumer<OffenderNumber> {
    }
}
