package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CompareFacesMatch;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteFacesRequest;
import software.amazon.awssdk.services.rekognition.model.Face;
import software.amazon.awssdk.services.rekognition.model.FaceRecord;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.IndexFacesRequest;
import software.amazon.awssdk.services.rekognition.model.IndexFacesResponse;
import software.amazon.awssdk.services.rekognition.model.SearchFacesRequest;
import uk.gov.justice.hmpps.datacompliance.utils.Result;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparingDouble;
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
                                     @Value("${image.recognition.aws.face.similarity.threshold}") final double faceSimilarityThreshold) {

        log.info("Configured to use AWS Rekognition for image recognition");

        this.client = client;
        this.collectionId = collectionId;
        this.faceSimilarityThreshold = faceSimilarityThreshold;
    }

    @Override
    public Result<FaceId, IndexFacesError> uploadImageToCollection(final OffenderImage image) {

        log.trace("Uploading image data for offender: '{}', image: '{}'",
                image.getOffenderNumberString(), image.getImageData());

        final var result = client.indexFaces(generateIndexFaceRequest(image));

        return ensureOnlyOneFaceIndexed(image, result);
    }

    @Override
    public Set<FaceMatch> findMatchesFor(final FaceId faceId) {

        log.trace("Finding face matches for faceId: '{}'", faceId.getFaceId());

        final var result = client.searchFaces(generateSearchFacesRequest(faceId));

        return result.hasFaceMatches() ? transformFaceMatches(result.faceMatches()) : emptySet();
    }

    @Override
    public Optional<Double> getSimilarity(final OffenderImage image1, final OffenderImage image2) {

        final var response = client.compareFaces(CompareFacesRequest.builder()
                .qualityFilter(HIGH)
                .sourceImage(Image.builder().bytes(SdkBytes.fromByteArray(image1.getImageData())).build())
                .targetImage(Image.builder().bytes(SdkBytes.fromByteArray(image2.getImageData())).build())
                .build());

        if (response.hasFaceMatches()) {
            return response.faceMatches().stream()
                    .map(CompareFacesMatch::similarity)
                    .map(Double::valueOf)
                    .max(comparingDouble(d -> d));
        }

        return Optional.empty();
    }

    private Result<FaceId, IndexFacesError> ensureOnlyOneFaceIndexed(final OffenderImage image,
                                                                     final IndexFacesResponse indexedFacesResponse) {

        final var indexedFaces = indexedFacesResponse.faceRecords();

        if (indexedFaces.isEmpty()) {
            return indexedFacesResponse.hasUnindexedFaces() ?
                    error(FACE_POOR_QUALITY) : error(FACE_NOT_FOUND);
        }

        if (indexedFaces.size() > 1) {
            return handleMultipleFaces(image, indexedFaces);
        }

        return indexedFaces.stream()
                .map(FaceRecord::face)
                .map(Face::faceId)
                .findFirst()
                .map(this::singleFaceId)
                .orElse(error(FACE_NOT_FOUND));
    }

    private IndexFacesRequest generateIndexFaceRequest(final OffenderImage image) {

        // Check for up to two faces in a single image so that we are warned
        // if multiple faces are detected.
        return IndexFacesRequest.builder()
                .collectionId(collectionId)
                .maxFaces(2)
                .qualityFilter(HIGH)
                .externalImageId(generateExternalImageId(image))
                .image(Image.builder().bytes(fromByteArray(image.getImageData())).build())
                .build();
    }

    private Result<FaceId, IndexFacesError> handleMultipleFaces(final OffenderImage image,
                                                                final List<FaceRecord> multipleFaces) {
        log.warn("Multiple faces (count: '{}') for offender: '{}' and image: '{}'",
                multipleFaces.size(), image.getOffenderNumberString(), image.getImageId());

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

    private String generateExternalImageId(final OffenderImage image) {
        return format("%s-%s", image.getOffenderNumberString(), image.getImageId());
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

    private Set<FaceMatch> transformFaceMatches(
            final List<software.amazon.awssdk.services.rekognition.model.FaceMatch> faceMatches) {

        return faceMatches.stream()
                .map(match -> new FaceMatch(new FaceId(match.face().faceId()), match.similarity()))
                .collect(toSet());
    }
}
