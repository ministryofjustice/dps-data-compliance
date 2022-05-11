package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.athena.model.EncryptionOption.SSE_S3;

class DuplicateDetectionQueryFactoryTest {

    private static final String DATABASE = "database1";
    private static final String TABLE = "table1";
    private static final String OUTPUT_LOCATION = "output1";
    private static final double MATCH_SCORE_THRESHOLD = 0.75;
    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final String QUERY_EXECUTION_ID = "query1";

    private DuplicateDetectionQueryFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DuplicateDetectionQueryFactory(DATABASE, TABLE, OUTPUT_LOCATION, MATCH_SCORE_THRESHOLD);
    }

    @Test
    void startQueryExecution() {
        final var request = factory.startQueryExecution(OFFENDER_NUMBER);

        assertThat(request.queryExecutionContext().database()).isEqualTo(DATABASE);
        assertThat(request.resultConfiguration().outputLocation()).isEqualTo(OUTPUT_LOCATION);
        assertThat(request.resultConfiguration().encryptionConfiguration().encryptionOption()).isEqualTo(SSE_S3);
        assertThat(request.queryString()).isEqualTo(
            "SELECT * FROM database1.table1 " +
                "WHERE (offender_id_display_l = 'A1234AA' OR offender_id_display_r = 'A1234AA') " +
                "AND match_score > 0.75");
    }

    @Test
    void startQueryExecutionEnsuresOffenderNumberIsValid() {

        final var badOffenderNumber = mock(OffenderNumber.class);
        when(badOffenderNumber.getOffenderNumber()).thenReturn("'OR 1=1; --");

        assertThatThrownBy(() -> factory.startQueryExecution(badOffenderNumber))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Offender number ''OR 1=1; --' does not match regex");
    }

    @Test
    void getQueryExecution() {
        final var request = factory.getQueryExecution(QUERY_EXECUTION_ID);
        assertThat(request.queryExecutionId()).isEqualTo(QUERY_EXECUTION_ID);
    }

    @Test
    void getQueryResults() {
        final var request = factory.getQueryResults(QUERY_EXECUTION_ID);
        assertThat(request.queryExecutionId()).isEqualTo(QUERY_EXECUTION_ID);
        assertThat(request.maxResults()).isEqualTo(100);
    }
}
