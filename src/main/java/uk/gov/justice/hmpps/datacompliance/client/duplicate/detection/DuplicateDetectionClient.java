package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.Set;

public interface DuplicateDetectionClient {
    Set<DuplicateResult> findDuplicatesFor(OffenderNumber offenderNumber);
}
