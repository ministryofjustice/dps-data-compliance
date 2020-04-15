package uk.gov.justice.hmpps.datacompliance.services.retention;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonPathfinder;

import java.util.List;

@Service
@AllArgsConstructor
public class RetentionService {

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;

    public List<RetentionReason> findRetentionReasons(final OffenderNumber offenderNumber) {

        // TODO GDPR-51 complete the following checks
        //  * Duplicates
        //  * Moratoria

        final var retentionReasons = ImmutableList.<RetentionReason>builder();

        if (pathfinderApiClient.isReferredToPathfinder(offenderNumber)) {
            retentionReasons.add(new RetentionReasonPathfinder());
        }

        manualRetentionService.findManualOffenderRetentionWithReasons(offenderNumber)
                .ifPresent(manualRetention ->
                        retentionReasons.add(new RetentionReasonManual().setManualRetention(manualRetention)));

        return retentionReasons.build();
    }
}
