package uk.gov.justice.hmpps.datacompliance.services.client.image.recognition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static software.amazon.awssdk.core.SdkBytes.fromByteArray;
import static software.amazon.awssdk.services.rekognition.model.QualityFilter.HIGH;

@Slf4j
@Component
@ConditionalOnProperty(name = "image.recognition.provider")
public class AwsImageRecognitionClient implements ImageRecognitionClient {

    private final String collectionId;
    private final RekognitionClient client;

    public AwsImageRecognitionClient(final RekognitionClient client,
                                     @Value("${image.recognition.aws.collection.id}") final String collectionId) {

        log.info("Configured to use AWS Rekognition for image recognition");

        this.client = client;
        this.collectionId = collectionId;
    }

    @Override
    public Optional<String> uploadImageToCollection(final byte[] imageData,
                                                    final OffenderNumber offenderNumber,
                                                    final long imageId) {

        log.debug("Uploading image data for offender: '{}', image: '{}'", offenderNumber.getOffenderNumber(), imageId);

        final var indexedFaces = client.indexFaces(
                generateIndexFaceRequest(imageData, offenderNumber, imageId))
                .faceRecords();

        return ensureOnlyOneFaceIndexed(offenderNumber, imageId, indexedFaces);
    }

    private Optional<String> ensureOnlyOneFaceIndexed(final OffenderNumber offenderNumber,
                                                      final long imageId,
                                                      final List<FaceRecord> indexedFaces) {

        if (indexedFaces.size() != 1) {

            log.warn("Face count: '{}' for offender: '{}' and image: '{}'",
                    indexedFaces.size(), offenderNumber.getOffenderNumber(), imageId);

            // Need to delete all indexed faces in this image because we cannot tell which one
            // belongs to the offender:
            indexedFaces.forEach(face -> removeFaceFromCollection(face.face().faceId()));

            return Optional.empty();
        }

        return indexedFaces.stream()
                .map(FaceRecord::face)
                .map(Face::faceId)
                .findFirst();
    }

    private IndexFacesRequest generateIndexFaceRequest(final byte[] imageData,
                                                       final OffenderNumber offenderNumber,
                                                       final long imageId) {
        // Check for up to two faces in a single image so that we are warned
        // if multiple faces are detected.
        return IndexFacesRequest.builder()
                .collectionId(collectionId)
                .maxFaces(2)
                .qualityFilter(HIGH)
                .externalImageId(generateExternalImageId(offenderNumber, imageId))
                .image(Image.builder().bytes(fromByteArray(imageData)).build())
                .build();
    }

    private void removeFaceFromCollection(String faceId) {

        log.debug("Removing face: '{}' from collection", faceId);

        client.deleteFaces(generateDeleteFaceRequest(faceId));
    }

    private DeleteFacesRequest generateDeleteFaceRequest(final String faceId) {
        return DeleteFacesRequest.builder()
                .collectionId(collectionId)
                .faceIds(faceId)
                .build();
    }

    private String generateExternalImageId(final OffenderNumber offenderNumber, final long imageId) {
        return format("%s-%s", offenderNumber.getOffenderNumber(), imageId);
    }
}
