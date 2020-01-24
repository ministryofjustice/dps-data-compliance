package uk.gov.justice.hmpps.datacompliance.services.client.image.recognition;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

import static com.amazonaws.services.rekognition.model.QualityFilter.HIGH;
import static java.lang.String.format;

@Slf4j
@Component
@ConditionalOnProperty(name = "image.recognition.provider")
public class AwsImageRecognitionClient implements ImageRecognitionClient {

    private final String collectionId;
    private final AmazonRekognition client;

    public AwsImageRecognitionClient(final AmazonRekognition client,
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
                .getFaceRecords();

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
            indexedFaces.forEach(face -> removeFaceFromCollection(face.getFace().getFaceId()));

            return Optional.empty();
        }

        return indexedFaces.stream()
                .map(FaceRecord::getFace)
                .map(Face::getFaceId)
                .findFirst();
    }

    private IndexFacesRequest generateIndexFaceRequest(final byte[] imageData,
                                                       final OffenderNumber offenderNumber,
                                                       final long imageId) {
        // Check for up to two faces in a single image so that we are warned
        // if multiple faces are detected.
        return new IndexFacesRequest()
                .withCollectionId(collectionId)
                .withMaxFaces(2)
                .withQualityFilter(HIGH)
                .withExternalImageId(generateExternalImageId(offenderNumber, imageId))
                .withImage(new Image().withBytes(ByteBuffer.wrap(imageData)));
    }

    private void removeFaceFromCollection(String faceId) {

        log.debug("Removing face: '{}' from collection", faceId);

        client.deleteFaces(generateDeleteFaceRequest(faceId));
    }

    private DeleteFacesRequest generateDeleteFaceRequest(final String faceId) {
        return new DeleteFacesRequest()
                .withCollectionId(collectionId)
                .withFaceIds(faceId);
    }

    private String generateExternalImageId(final OffenderNumber offenderNumber, final long imageId) {
        return format("%s-%s", offenderNumber.getOffenderNumber(), imageId);
    }
}
