package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class DuplicateResult {
    private final OffenderNumber duplicateOffenderNumber;
    private final double confidence;
}
