package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.athena.model.EncryptionConfiguration;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static software.amazon.awssdk.services.athena.model.EncryptionOption.SSE_S3;

@Slf4j
@Component
@ConditionalOnProperty(name = "duplicate.detection.provider")
public class DuplicateDetectionQueryFactory {

    private static final int MAX_RESULTS = 100;
    private static final String QUERY_TEMPLATE =
            "SELECT * FROM %s.%s " +
            "WHERE (offender_id_display_l = '%s' OR offender_id_display_r = '%<s') " +
            "AND match_score > %s";

    private final String database;
    private final String table;
    private final String outputLocation;
    private final double matchScoreThreshold;

    public DuplicateDetectionQueryFactory(@Value("${duplicate.detection.athena.database}") final String database,
                                          @Value("${duplicate.detection.athena.table}") final String table,
                                          @Value("${duplicate.detection.athena.output.location}") final String outputLocation,
                                          @Value("${duplicate.detection.match.score.threshold:0.75}") final double matchScoreThreshold) {
        this.database = database;
        this.table = table;
        this.outputLocation = outputLocation;
        this.matchScoreThreshold = matchScoreThreshold;
    }


    StartQueryExecutionRequest startQueryExecution(final OffenderNumber offenderNum) {

        // Should not be allowed to happen, but double check:
        final var offenderNumber = offenderNum.getOffenderNumber();

        checkArgument(OffenderNumber.isValid(offenderNumber),
                "Offender number '%s' does not match regex", offenderNumber);

        return StartQueryExecutionRequest.builder()
                .queryString(constructSql(offenderNumber))
                .queryExecutionContext(queryExecutionContext())
                .resultConfiguration(resultConfiguration())
                .build();
    }

    GetQueryExecutionRequest getQueryExecution(final String queryExecutionId) {
        return GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build();
    }

    GetQueryResultsRequest getQueryResults(final String queryExecutionId) {
        return GetQueryResultsRequest.builder()
                .queryExecutionId(queryExecutionId)
                .maxResults(MAX_RESULTS)
                .build();
    }

    private String constructSql(final String offenderNumber) {
        return format(
                QUERY_TEMPLATE,
                database,
                table,
                offenderNumber,
                String.valueOf(matchScoreThreshold).replaceAll("[.]?[0]*$", ""));
    }

    private QueryExecutionContext queryExecutionContext() {
        return QueryExecutionContext.builder().database(database).build();
    }

    private ResultConfiguration resultConfiguration() {
        return ResultConfiguration.builder()
                .outputLocation(outputLocation)
                .encryptionConfiguration(EncryptionConfiguration.builder()
                        .encryptionOption(SSE_S3)
                        .build())
                .build();
    }
}
