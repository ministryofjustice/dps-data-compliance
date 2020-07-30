package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class OffenderToCheck {
    private final OffenderNumber offenderNumber;
    @Singular private final List<String> offenceCodes;
    @Singular private final List<String> alertCodes;
}
