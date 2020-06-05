package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.Math.pow;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static software.amazon.awssdk.services.athena.model.QueryExecutionState.QUEUED;
import static software.amazon.awssdk.services.athena.model.QueryExecutionState.RUNNING;
import static software.amazon.awssdk.services.athena.model.QueryExecutionState.SUCCEEDED;

@Slf4j
@Component
@ConditionalOnProperty(name = "duplicate.detection.provider")
public class AwsAthenaDuplicateDetectionClient implements DuplicateDetectionClient {

    private static final Set<QueryExecutionState> ATHENA_WAITING_STATES = Set.of(QUEUED, RUNNING);
    private static final int WAIT_TIME_MULTIPLIER = 2;
    private static final int MAX_WAIT_ATTEMPTS = 8;

    private final AthenaClient athenaClient;
    private final DuplicateDetectionQueryFactory queryFactory;
    private final Duration initialWaitDuration;

    AwsAthenaDuplicateDetectionClient(final AthenaClient athenaClient,
                                      final DuplicateDetectionQueryFactory queryFactory,
                                      @Value("${duplicate.detection.athena.initial.wait:500ms}") final Duration initialWaitDuration) {

        log.info("Configured to use AWS Athena for querying for offender duplicates");

        this.athenaClient = athenaClient;
        this.initialWaitDuration = initialWaitDuration;
        this.queryFactory = queryFactory;
    }

    @Override
    public Set<DuplicateResult> findDuplicatesFor(final OffenderNumber offenderNumber) {

        final var queryExecutionId = submitAthenaQuery(offenderNumber);

        awaitResponse(queryExecutionId);

        return retrieveResult(queryExecutionId, offenderNumber);
    }

    private String submitAthenaQuery(final OffenderNumber offenderNumber) {
        return athenaClient.startQueryExecution(queryFactory.startQueryExecution(offenderNumber))
                .queryExecutionId();
    }

    private void awaitResponse(final String queryExecutionId) {

        final var attemptCounter = new AtomicInteger();

        while (attemptCounter.getAndIncrement() < MAX_WAIT_ATTEMPTS) {

            final var executionDetails = athenaClient.getQueryExecution(queryFactory.getQueryExecution(queryExecutionId));
            final var queryStatus = executionDetails.queryExecution().status();

            if (queryStatus.state() == SUCCEEDED) {
                log.debug("Query {} has completed", queryExecutionId);
                return;
            }

            if (ATHENA_WAITING_STATES.contains(queryStatus.state())) {
                wait(attemptCounter);
                continue;
            }

            throw new IllegalStateException(format("Athena query '%s' has failed with reason: '%s', '%s'",
                    queryExecutionId, queryStatus.state(), queryStatus.stateChangeReason()));
        }

        throw new IllegalStateException(format("Athena query '%s' has timed out", queryExecutionId));
    }

    public Set<DuplicateResult> retrieveResult(final String queryExecutionId, final OffenderNumber referenceOffender) {

        return athenaClient.getQueryResultsPaginator(queryFactory.getQueryResults(queryExecutionId)).stream()
                .flatMap(resultPage -> getDuplicatesFromPagedResult(resultPage, referenceOffender))
                .collect(toSet());
    }

    private void wait(final AtomicInteger numberOfAttempts) {

        final var waitTime = initialWaitDuration.multipliedBy((long) pow(WAIT_TIME_MULTIPLIER, numberOfAttempts.get()));

        try {
            Thread.sleep(waitTime.toMillis());
        } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new IllegalStateException("Wait for Athena query interrupted");
        }
    }

    private Stream<DuplicateResult> getDuplicatesFromPagedResult(final GetQueryResultsResponse resultPage,
                                                                 final OffenderNumber referenceOffender) {
        return  resultPage.resultSet().rows().stream()
                .map(DuplicateOffenderRow::new)
                .filter(not(DuplicateOffenderRow::isHeaderRow))
                .map(row -> new DuplicateResult(row.getComplementOf(referenceOffender), row.getMatchScore()));
    }
}
