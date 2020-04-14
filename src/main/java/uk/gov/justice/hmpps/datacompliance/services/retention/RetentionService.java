package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;

import java.util.Optional;

@Service
@AllArgsConstructor
public class RetentionService {

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;

    public Optional<RetentionReason> findRetentionReason(final OffenderNumber offenderNumber) {

        // TODO GDPR-51 complete the following checks
        //  * Duplicates
        //  * Moratoria

        final var isReferredToPathfinder = pathfinderApiClient.isReferredToPathfinder(offenderNumber);
        final var manualRetention = manualRetentionService.findManualOffenderRetentionWithReasons(offenderNumber);

        if (!isReferredToPathfinder && manualRetention.isEmpty()) {
            return Optional.empty();
        }

        final var retentionReason = RetentionReason.builder()
                .pathfinderReferred(isReferredToPathfinder);

        manualRetention.ifPresent(retentionReason::manualRetention);

        return Optional.of(retentionReason.build());
    }
}
