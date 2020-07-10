package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class FalsePositiveCheckService {

    private final PrisonApiClient prisonApiClient;
    private final ImageDuplicationDetectionService imageDuplicationDetectionService;
    private final DataComplianceProperties properties;

    public FalsePositiveCheckService(final PrisonApiClient prisonApiClient,
                                     final ImageDuplicationDetectionService imageDuplicationDetectionService,
                                     final DataComplianceProperties properties) {
        this.prisonApiClient = prisonApiClient;
        this.imageDuplicationDetectionService = imageDuplicationDetectionService;
        this.properties = properties;
    }

    public boolean isFalsePositive(final RetentionCheckDataDuplicate check) {

        if (!properties.isFalsePositiveDuplicateCheckEnabled()) {
            log.debug("Not configured to perform false positive check for '{}'", check.getRetentionCheckId());
            return false;
        }

        log.debug("Checking if check: '{}' is a false positive", check.getRetentionCheckId());

        return check.getDataDuplicates().stream().allMatch(this::sufficientImagesAndAllDissimilar);
    }

    private boolean sufficientImagesAndAllDissimilar(final DataDuplicate duplicate) {

        final var requiredImageCount = properties.getFalsePositiveDuplicateRequiredImageCount();
        final var referenceOffenderImages = getImagesFor(new OffenderNumber(duplicate.getReferenceOffenderNo()));
        final var duplicateOffenderImages = getImagesFor(new OffenderNumber(duplicate.getDuplicateOffenderNo()));

        if (referenceOffenderImages.size() < requiredImageCount || duplicateOffenderImages.size() < requiredImageCount) {
            log.debug("Number of images is not sufficient to check duplicate: '{}'", duplicate.getDataDuplicateId());
            return false;
        }

        return referenceOffenderImages.stream()
                .allMatch(referenceImage -> noSimilarity(referenceImage, duplicateOffenderImages));
    }

    private boolean noSimilarity(final OffenderImage referenceImage, final Collection<OffenderImage> comparisonImages) {

        final var successfulComparisons = comparisonImages.stream()
                .map(comparisonImage -> imageDuplicationDetectionService.getSimilarity(referenceImage, comparisonImage))
                .flatMap(Optional::stream)
                .collect(toList());

        return successfulComparisons.size() == comparisonImages.size() &&
                successfulComparisons.stream().allMatch(similarity ->
                        similarity < properties.getFalsePositiveDuplicateImageSimilarityThreshold());
    }

    private List<OffenderImage> getImagesFor(final OffenderNumber offenderNumber) {
        return prisonApiClient.getOffenderFaceImagesFor(offenderNumber).stream()
                .map(metadata -> prisonApiClient.getImageData(offenderNumber, metadata.getImageId()))
                .flatMap(Optional::stream)
                .collect(toList());
    }
}
