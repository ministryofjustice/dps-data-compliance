package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.Row;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplicateOffenderRowTest {

    private static final OffenderNumber OFFENDER_1 = new OffenderNumber("A1234AA");
    private static final OffenderNumber OFFENDER_2 = new OffenderNumber("B1234BB");
    private static final OffenderNumber OFFENDER_3 = new OffenderNumber("C1234CC");

    @Test
    void headerRow() {

        final var row = new DuplicateOffenderRow(Row.builder()
                .data(
                        Datum.builder().varCharValue("offender_id_display_l").build(),
                        Datum.builder().varCharValue("offender_id_display_r").build(),
                        Datum.builder().varCharValue("match_score").build())
                .build());

        assertThat(row.isHeaderRow()).isTrue();
        assertThatThrownBy(row::getMatchScore).isInstanceOf(IllegalStateException.class).hasMessage("Cannot retrieve value from header row");
        assertThatThrownBy(() -> row.getComplementOf(OFFENDER_1)).isInstanceOf(IllegalStateException.class).hasMessage("Cannot retrieve value from header row");
    }

    @Test
    void dataRow() {

        final var row = new DuplicateOffenderRow(Row.builder()
                .data(
                        Datum.builder().varCharValue(OFFENDER_1.getOffenderNumber()).build(),
                        Datum.builder().varCharValue(OFFENDER_2.getOffenderNumber()).build(),
                        Datum.builder().varCharValue("0.123").build())
                .build());

        assertThat(row.isHeaderRow()).isFalse();
        assertThat(row.getMatchScore()).isEqualTo(0.123);
        assertThat(row.getComplementOf(OFFENDER_1)).isEqualTo(OFFENDER_2);
        assertThat(row.getComplementOf(OFFENDER_2)).isEqualTo(OFFENDER_1);
    }

    @Test
    void getComplementThrowsIfOffenderNumberNotPresent() {

        final var row = new DuplicateOffenderRow(Row.builder()
                .data(
                        Datum.builder().varCharValue(OFFENDER_1.getOffenderNumber()).build(),
                        Datum.builder().varCharValue(OFFENDER_2.getOffenderNumber()).build(),
                        Datum.builder().varCharValue("0.123").build())
                .build());

        assertThatThrownBy(() -> row.getComplementOf(OFFENDER_3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Neither offender in the row (A1234AA / B1234BB) matches the intended reference offender number: 'C1234CC'");
    }
}
