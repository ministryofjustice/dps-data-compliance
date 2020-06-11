package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class DuplicateResult {
    private final OffenderNumber duplicateOffenderNumber;
    private final Double confidence;
}
