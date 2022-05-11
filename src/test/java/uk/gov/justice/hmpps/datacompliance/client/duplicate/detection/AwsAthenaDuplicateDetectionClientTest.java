package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;
import uk.gov.justice.hmpps.datacompliance.dto.DuplicateResult;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.athena.model.QueryExecutionState.FAILED;
import static software.amazon.awssdk.services.athena.model.QueryExecutionState.RUNNING;
import static software.amazon.awssdk.services.athena.model.QueryExecutionState.SUCCEEDED;

@ExtendWith(MockitoExtension.class)
class AwsAthenaDuplicateDetectionClientTest {

    private static final OffenderNumber REFERENCE_OFFENDER = new OffenderNumber("A1234AA");
    private static final OffenderNumber DUPLICATE_OFFENDER_1 = new OffenderNumber("B1234BB");
    private static final OffenderNumber DUPLICATE_OFFENDER_2 = new OffenderNumber("C1234CC");
    private static final String QUERY_EXECUTION_ID = "query1";
    private static final double MATCH_SCORE_1 = 0.8;
    private static final double CONFIDENCE_1 = MATCH_SCORE_1 * 100;
    private static final double MATCH_SCORE_2 = 0.9;
    private static final double CONFIDENCE_2 = MATCH_SCORE_2 * 100;

    @Mock
    private DuplicateDetectionQueryFactory queryFactory;

    @Mock
    private AthenaClient athenaClient;

    private DuplicateDetectionClient client;

    @AfterAll
    static void afterAll() {
        Mockito.framework().clearInlineMocks();
    }

    @BeforeEach
    void setUp() {
        client = new AwsAthenaDuplicateDetectionClient(athenaClient, queryFactory, Duration.ZERO);
    }

    @Test
    void findDuplicates() {

        mockStartQueryExecutionExchange();
        mockExecutionState(SUCCEEDED);
        mockGetQueryResultsExchange();

        assertThat(client.findDuplicatesFor(REFERENCE_OFFENDER))
            .containsExactlyInAnyOrder(
                new DuplicateResult(DUPLICATE_OFFENDER_1, CONFIDENCE_1),
                new DuplicateResult(DUPLICATE_OFFENDER_2, CONFIDENCE_2));
    }

    @Test
    void findDuplicatesThrowsUponPollingTimeout() {

        mockStartQueryExecutionExchange();
        mockExecutionState(RUNNING);

        assertThatThrownBy(() -> client.findDuplicatesFor(REFERENCE_OFFENDER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Athena query 'query1' has timed out");
    }

    @Test
    void findDuplicatesThrowsUponExecutionFailure() {

        mockStartQueryExecutionExchange();
        mockExecutionState(FAILED, "Some error");

        assertThatThrownBy(() -> client.findDuplicatesFor(REFERENCE_OFFENDER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Athena query 'query1' has failed with reason: 'FAILED', 'Some error'");
    }

    private void mockStartQueryExecutionExchange() {
        final var startQueryExecution = StartQueryExecutionRequest.builder().build();
        when(queryFactory.startQueryExecution(REFERENCE_OFFENDER)).thenReturn(startQueryExecution);
        when(athenaClient.startQueryExecution(startQueryExecution)).thenReturn(StartQueryExecutionResponse.builder()
            .queryExecutionId(QUERY_EXECUTION_ID)
            .build());
    }

    private void mockExecutionState(final QueryExecutionState state) {
        mockExecutionState(state, null);
    }

    private void mockExecutionState(final QueryExecutionState state, final String stateChangeReason) {
        final var getQueryExecution = GetQueryExecutionRequest.builder().build();
        when(queryFactory.getQueryExecution(QUERY_EXECUTION_ID)).thenReturn(getQueryExecution);
        when(athenaClient.getQueryExecution(getQueryExecution)).thenReturn(GetQueryExecutionResponse.builder()
            .queryExecution(QueryExecution.builder()
                .status(QueryExecutionStatus.builder()
                    .state(state)
                    .stateChangeReason(stateChangeReason)
                    .build())
                .build())
            .build());
    }

    private void mockGetQueryResultsExchange() {
        final var getQueryResults = GetQueryResultsRequest.builder().build();
        when(queryFactory.getQueryResults(QUERY_EXECUTION_ID)).thenReturn(getQueryResults);
        when(athenaClient.getQueryResultsPaginator(getQueryResults)).thenReturn(new GetQueryResultsIterable(athenaClient, getQueryResults));
        when(athenaClient.getQueryResults(getQueryResults)).thenReturn(GetQueryResultsResponse.builder()
            .resultSet(ResultSet.builder()
                .rows(
                    headerRow(),
                    dataRow(REFERENCE_OFFENDER, DUPLICATE_OFFENDER_1, MATCH_SCORE_1),
                    dataRow(DUPLICATE_OFFENDER_2, REFERENCE_OFFENDER, MATCH_SCORE_2)) // offenders may be either way round
                .build())
            .build());
    }

    private Row headerRow() {
        return Row.builder()
            .data(
                Datum.builder().varCharValue("offender_id_display_l").build(),
                Datum.builder().varCharValue("offender_id_display_r").build(),
                Datum.builder().varCharValue("match_score").build())
            .build();
    }

    private Row dataRow(final OffenderNumber offender1, final OffenderNumber offender2, final double matchScore) {
        return Row.builder()
            .data(
                Datum.builder().varCharValue(offender1.getOffenderNumber()).build(),
                Datum.builder().varCharValue(offender2.getOffenderNumber()).build(),
                Datum.builder().varCharValue(String.valueOf(matchScore)).build())
            .build();
    }
}
