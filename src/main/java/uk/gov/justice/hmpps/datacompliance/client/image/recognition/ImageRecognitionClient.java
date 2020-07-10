package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import uk.gov.justice.hmpps.datacompliance.utils.Result;

import java.util.Optional;
import java.util.Set;

public interface ImageRecognitionClient {

    /**
     * Upload image and index face into collection for facial recognition.
     * @return Result object containing server-side unique identifier for the face recognised in the image or an
     * enumerated error.
     */
    Result<FaceId, IndexFacesError> uploadImageToCollection(OffenderImage offenderImage);

    /**
     * Find all matching faces in the collection that match the provided faceId
     */
    Set<FaceMatch> findMatchesFor(FaceId faceId);

    /**
     * Compare two images and return the strongest similarity between faces
     * in those images (value 0.0 - 100.0).
     */
    Optional<Double> getSimilarity(OffenderImage image1, OffenderImage image2);
}
