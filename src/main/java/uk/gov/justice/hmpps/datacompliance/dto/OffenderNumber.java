package uk.gov.justice.hmpps.datacompliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import static java.util.Objects.requireNonNull;

@Data
public class OffenderNumber {

    private final String offenderNumber;

    public OffenderNumber(@JsonProperty("offenderNumber") final String offenderNumber) {
        requireNonNull(offenderNumber, "Null offender number");
        this.offenderNumber = offenderNumber;
    }
}
