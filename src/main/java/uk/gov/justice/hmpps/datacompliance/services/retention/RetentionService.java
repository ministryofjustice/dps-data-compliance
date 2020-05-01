package uk.gov.justice.hmpps.datacompliance.services.retention;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonPathfinder;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@AllArgsConstructor
public class RetentionService {

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;
    private final ImageDuplicationDetectionService imageDuplicationDetectionService;
    private final DataDuplicationDetectionService dataDuplicationDetectionService;

    public List<RetentionReason> findRetentionReasons(final OffenderNumber offenderNumber) {

        // TODO GDPR-51 complete the following checks
        //  * Moratoria

        return ImmutableList.<RetentionReason>builder()
                .addAll(pathfinderReferral(offenderNumber))
                .addAll(manualRetention(offenderNumber))
                .addAll(potentialDuplicates(offenderNumber))
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

    private List<RetentionReason> potentialDuplicates(final OffenderNumber offenderNumber) {
        final var dataDuplicates = dataDuplicationDetectionService.findDuplicatesFor(offenderNumber);
        final var imageDuplicates = imageDuplicationDetectionService.findDuplicatesFor(offenderNumber);

        if (dataDuplicates.isEmpty() && imageDuplicates.isEmpty()) {
            log.info("No duplicate found for offender: '{}'", offenderNumber);
            return emptyList();
        }

        return List.of(
                new RetentionReasonDuplicate()
                        .addDataDuplicates(dataDuplicates)
                        .addImageDuplicates(imageDuplicates));
    }
}
