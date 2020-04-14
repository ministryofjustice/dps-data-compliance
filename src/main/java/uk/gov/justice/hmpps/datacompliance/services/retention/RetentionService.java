package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;

import java.util.Optional;

@Service
@AllArgsConstructor
public class RetentionService {

    private final ManualRetentionService manualRetentionService;

    public Optional<RetentionReason> findRetentionReason(final OffenderNumber offenderNumber) {

        // TODO GDPR-51 complete the following checks
        //  * Pathfinder
        //  * Duplicates
        //  * Moratoria

        return manualRetentionService.findManualOffenderRetentionWithReasons(offenderNumber)
                .map(manualRetention ->
                        RetentionReason.builder()
                                .manualRetention(manualRetention)
                                .build());
    }
}
