package uk.gov.justice.hmpps.datacompliance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import static java.util.Objects.requireNonNull;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenderNumber {

    private final String offenderNumber;

    public OffenderNumber(@JsonProperty("offenderNumber") final String offenderNumber) {
        requireNonNull(offenderNumber, "Null offender number");
        this.offenderNumber = offenderNumber;
    }
}
