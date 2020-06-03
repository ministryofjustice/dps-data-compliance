package uk.gov.justice.hmpps.datacompliance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenderNumber {

    private static final String OFFENDER_NUMBER_REGEX = "^[A-Z][0-9]{4}[A-Z]{2}$";

    private final String offenderNumber;

    public OffenderNumber(@JsonProperty("offenderNumber") final String offenderNumber) {
        requireNonNull(offenderNumber, "Null offender number");
        checkArgument(isValid(offenderNumber), "Invalid offender number: '%s'", offenderNumber);
        this.offenderNumber = offenderNumber;
    }

    public static boolean isValid(final String offenderNumber) {
        return offenderNumber.matches(OFFENDER_NUMBER_REGEX);
    }
}
