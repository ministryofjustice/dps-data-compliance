package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.athena.model.Row;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Double.parseDouble;

@AllArgsConstructor
public class DuplicateOffenderRow {

    private static final String COLUMN1_HEADING = "offender_id_display_l";
    private final Row row;

    public boolean isHeaderRow() {
        return COLUMN1_HEADING.equalsIgnoreCase(row.data().get(0).varCharValue());
    }

    public double getMatchScore() {
        checkValueRow();
        return parseDouble(row.data().get(2).varCharValue());
    }

    public OffenderNumber getComplementOf(final OffenderNumber offenderNumber) {
        final var matchToOffender1 = Objects.equals(offenderNumber, getOffender1());
        final var matchToOffender2 = Objects.equals(offenderNumber, getOffender2());

        checkState(matchToOffender1 || matchToOffender2,
            "Neither offender in the row (%s / %s) matches the intended reference offender number: '%s'",
            getOffender1().getOffenderNumber(), getOffender2().getOffenderNumber(), offenderNumber.getOffenderNumber());

        return matchToOffender1 ? getOffender2() : getOffender1();
    }

    private OffenderNumber getOffender1() {
        checkValueRow();
        return new OffenderNumber(row.data().get(0).varCharValue());
    }

    private OffenderNumber getOffender2() {
        checkValueRow();
        return new OffenderNumber(row.data().get(1).varCharValue());
    }

    private void checkValueRow() {
        checkState(!isHeaderRow(), "Cannot retrieve value from header row");
    }
}
