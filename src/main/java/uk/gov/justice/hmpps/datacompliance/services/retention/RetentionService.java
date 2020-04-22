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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class RetentionService {

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;

    public List<RetentionReason> findRetentionReasons(final OffenderNumber offenderNumber) {

        // TODO GDPR-51 complete the following checks
        //  * Duplicates
        //  * Moratoria

        return ImmutableList.<RetentionReason>builder()
                .addAll(pathfinderReferral(offenderNumber))
                .addAll(manualRetention(offenderNumber))
                .build();
    }

    private List<RetentionReason> pathfinderReferral(final OffenderNumber offenderNumber) {
        return pathfinderApiClient.isReferredToPathfinder(offenderNumber)
                ? List.of(new RetentionReasonPathfinder())
                : emptyList();
    }

    private List<RetentionReason> manualRetention(final OffenderNumber offenderNumber) {
        return manualRetentionService.findManualOffenderRetentionWithReasons(offenderNumber)
                .map(manualRetention -> new RetentionReasonManual().setManualRetention(manualRetention))
                .stream().collect(toList());
    }
}
