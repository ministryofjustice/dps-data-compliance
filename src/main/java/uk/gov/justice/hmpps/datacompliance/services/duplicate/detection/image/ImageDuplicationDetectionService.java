package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ImageDuplicationDetectionService {

    private final ImageRecognitionClient imageRecognitionClient;
    private final OffenderImageUploadRepository imageUploadRepository;
    private final ImageDuplicateRepository imageDuplicateRepository;
    private final TimeSource timeSource;

    public List<ImageDuplicate> findDuplicatesFor(final OffenderNumber offenderNumber) {

        log.info("Finding image duplicates for offender: '{}'", offenderNumber.getOffenderNumber());

        final var imageUploads = imageUploadRepository.findByOffenderNo(offenderNumber.getOffenderNumber());

        return imageUploads.stream()
                .flatMap(this::findDuplicates)
                .collect(toList());
    }

    private Stream<ImageDuplicate> findDuplicates(final OffenderImageUpload referenceImage) {

        log.debug("Finding image duplicates for offender: '{}' and image: '{}'",
                referenceImage.getOffenderNo(), referenceImage.getImageId());

        final var matchingFaceIds = imageRecognitionClient.findMatchesFor(new FaceId(referenceImage.getFaceId()));

        return matchingFaceIds.stream()
                .map(matchingFaceId -> getImagePair(referenceImage, matchingFaceId))
                .filter(ImagePair::haveDifferentOffenderNumbers)
                .map(this::findOrPersistDuplicate);
    }

    private ImagePair getImagePair(final OffenderImageUpload referenceImage, final FaceId matchingFaceId) {

        log.debug("Duplicate face ('{}') found for offender: '{}' and image: '{}'",
                matchingFaceId, referenceImage.getOffenderNo(), referenceImage.getImageId());

        final var matchingImage = imageUploadRepository.findByFaceId(matchingFaceId.getFaceId())
                .orElseThrow(illegalState("Cannot find image upload for faceId: '%s'", matchingFaceId.getFaceId()));

        return new ImagePair(referenceImage, matchingImage);
    }

    private ImageDuplicate findOrPersistDuplicate(final ImagePair imagePair) {

        log.info("Image duplicate found for reference offender: '{}', and duplicate offender: '{}'",
                imagePair.getReferenceOffenderNo(), imagePair.getDuplicateOffenderNo());

        return imageDuplicateRepository.findByOffenderImageUploadIds(imagePair.getReferenceImageId(), imagePair.getDuplicateImageId())
                .orElseGet(() -> persistDuplicate(imagePair.getReferenceImage(), imagePair.getDuplicateImage()));
    }

    private ImageDuplicate persistDuplicate(final OffenderImageUpload referenceImage,
                                            final OffenderImageUpload duplicateImage) {
        return imageDuplicateRepository.save(ImageDuplicate.builder()
                .referenceOffenderImageUpload(referenceImage)
                .duplicateOffenderImageUpload(duplicateImage)
                .detectionDateTime(timeSource.nowAsLocalDateTime())
                .build());
    }

    @Getter
    @AllArgsConstructor
    private static class ImagePair {
        private final OffenderImageUpload referenceImage;
        private final OffenderImageUpload duplicateImage;

        private String getReferenceOffenderNo() {
            return referenceImage.getOffenderNo();
        }

        private String getDuplicateOffenderNo() {
            return duplicateImage.getOffenderNo();
        }

        private long getReferenceImageId() {
            return referenceImage.getImageId();
        }

        private long getDuplicateImageId() {
            return duplicateImage.getImageId();
        }

        private boolean haveDifferentOffenderNumbers() {
            return !Objects.equals(referenceImage.getOffenderNo(), duplicateImage.getOffenderNo());
        }
    }
}
