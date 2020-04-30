package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DeleteFacesRequest;
import software.amazon.awssdk.services.rekognition.model.Face;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.FaceRecord;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.IndexFacesRequest;
import software.amazon.awssdk.services.rekognition.model.IndexFacesResponse;
import software.amazon.awssdk.services.rekognition.model.SearchFacesRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.utils.Result;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static software.amazon.awssdk.core.SdkBytes.fromByteArray;
import static software.amazon.awssdk.services.rekognition.model.QualityFilter.HIGH;
import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.FACE_NOT_FOUND;
import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.FACE_POOR_QUALITY;
import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.MULTIPLE_FACES_FOUND;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.error;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.success;

@Slf4j
@Component
@ConditionalOnProperty(name = "image.recognition.provider")
public class AwsImageRecognitionClient implements ImageRecognitionClient {

    private final String collectionId;
    private final double faceSimilarityThreshold;
    private final RekognitionClient client;

    public AwsImageRecognitionClient(final RekognitionClient client,
                                     @Value("${image.recognition.aws.collection.id}") final String collectionId,
                                     @Value("${image.recognition.aws.face.similarity.threshold:95.0}") final double faceSimilarityThreshold) {

        log.info("Configured to use AWS Rekognition for image recognition");

        this.client = client;
        this.collectionId = collectionId;
        this.faceSimilarityThreshold = faceSimilarityThreshold;
    }

    @Override
    public Result<FaceId, IndexFacesError> uploadImageToCollection(final byte[] imageData,
                                                                   final OffenderNumber offenderNumber,
                                                                   final long imageId) {

        log.trace("Uploading image data for offender: '{}', image: '{}'", offenderNumber.getOffenderNumber(), imageId);

        final var result = client.indexFaces(
                generateIndexFaceRequest(imageData, offenderNumber, imageId));

        return ensureOnlyOneFaceIndexed(offenderNumber, imageId, result);
    }

    @Override
    public Set<FaceId> findMatchesFor(final FaceId faceId) {

        log.trace("Finding face matches for faceId: '{}'", faceId.getFaceId());

        final var result = client.searchFaces(generateSearchFacesRequest(faceId));

        return result.hasFaceMatches() ? transformFaceMatches(result.faceMatches()) : emptySet();
    }

    private Result<FaceId, IndexFacesError> ensureOnlyOneFaceIndexed(final OffenderNumber offenderNumber,
                                                                     final long imageId,
                                                                     final IndexFacesResponse indexedFacesResponse) {

        final var indexedFaces = indexedFacesResponse.faceRecords();

        if (indexedFaces.isEmpty()) {
            return indexedFacesResponse.hasUnindexedFaces() ?
                    error(FACE_POOR_QUALITY) : error(FACE_NOT_FOUND);
        }

        if (indexedFaces.size() > 1) {
            return handleMultipleFaces(offenderNumber, imageId, indexedFaces);
        }

        return indexedFaces.stream()
                .map(FaceRecord::face)
                .map(Face::faceId)
                .findFirst()
                .map(this::singleFaceId)
                .orElse(error(FACE_NOT_FOUND));
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

    private Result<FaceId, IndexFacesError> handleMultipleFaces(final OffenderNumber offenderNumber,
                                                                final long imageId,
                                                                final List<FaceRecord> multipleFaces) {
        log.warn("Multiple faces (count: '{}') for offender: '{}' and image: '{}'",
                multipleFaces.size(), offenderNumber.getOffenderNumber(), imageId);

        // Need to delete all indexed faces in this image because we cannot tell which one
        // belongs to the offender:
        multipleFaces.forEach(face -> removeFaceFromCollection(face.face().faceId()));

        return error(MULTIPLE_FACES_FOUND);
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

    private Result<FaceId, IndexFacesError> singleFaceId(final String faceId) {
        return success(new FaceId(faceId));
    }

    private SearchFacesRequest generateSearchFacesRequest(final FaceId faceId) {
        return SearchFacesRequest.builder()
                .collectionId(collectionId)
                .faceId(faceId.getFaceId())
                .faceMatchThreshold((float) faceSimilarityThreshold)
                .build();
    }

    private Set<FaceId> transformFaceMatches(final List<FaceMatch> faceMatches) {
        return faceMatches.stream()
                .map(FaceMatch::face)
                .map(Face::faceId)
                .map(FaceId::new)
                .collect(toSet());
    }
}
