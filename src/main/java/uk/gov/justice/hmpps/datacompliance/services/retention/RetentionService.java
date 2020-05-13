package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;

import java.util.List;

import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

@Slf4j
@Service
@AllArgsConstructor
public class RetentionService {

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;
    private final ImageDuplicationDetectionService imageDuplicationDetectionService;

    public List<RetentionCheck> conductRetentionChecks(final OffenderNumber offenderNumber) {

        // TODO GDPR-112 complete the following checks
        //  * Data duplicate check
        //  * Moratoria

        return List.of(
                pathfinderReferralCheck(offenderNumber),
                manualRetentionCheck(offenderNumber),
                imageDuplicateCheck(offenderNumber));
    }

    private RetentionCheck pathfinderReferralCheck(final OffenderNumber offenderNumber) {
        return new RetentionCheckPathfinder(pathfinderApiClient.isReferredToPathfinder(offenderNumber) ?
                RETENTION_REQUIRED : RETENTION_NOT_REQUIRED);
    }

    private RetentionCheck manualRetentionCheck(final OffenderNumber offenderNumber) {
        return manualRetentionService.findManualOffenderRetentionWithReasons(offenderNumber)
                .map(manualRetention -> new RetentionCheckManual(RETENTION_REQUIRED).setManualRetention(manualRetention))
                .orElseGet(() -> new RetentionCheckManual(RETENTION_NOT_REQUIRED));
    }

    private RetentionCheck imageDuplicateCheck(final OffenderNumber offenderNumber) {

        final var imageDuplicates = imageDuplicationDetectionService.findDuplicatesFor(offenderNumber);

        return imageDuplicates.isEmpty() ?
                new RetentionCheckImageDuplicate(RETENTION_NOT_REQUIRED) :
                new RetentionCheckImageDuplicate(RETENTION_REQUIRED).addImageDuplicates(imageDuplicates);
    }
}
