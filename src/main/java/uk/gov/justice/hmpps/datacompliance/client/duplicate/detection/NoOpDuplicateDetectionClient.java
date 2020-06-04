package uk.gov.justice.hmpps.datacompliance.client.duplicate.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.Set;

import static java.util.Collections.emptySet;

@Slf4j
@Component
@ConditionalOnProperty(value = "duplicate.detection.provider", matchIfMissing = true, havingValue = "no value set")
public class NoOpDuplicateDetectionClient implements DuplicateDetectionClient {

    public NoOpDuplicateDetectionClient() {
        log.info("Configured to ignore duplicate detection requests");
    }

    @Override
    public Set<DuplicateResult> findDuplicatesFor(OffenderNumber offenderNumber) {
        log.warn("Pretending to find duplicates for offender: '{}'", offenderNumber.getOffenderNumber());
        return emptySet();
    }
}
