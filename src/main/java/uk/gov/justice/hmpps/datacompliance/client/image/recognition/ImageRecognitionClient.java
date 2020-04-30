package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.utils.Result;

import java.util.Set;

public interface ImageRecognitionClient {

    /**
     * Upload image and index face into collection for facial recognition.
     * @return Result object containing server-side unique identifier for the face recognised in the image or an
     * enumerated error.
     */
    Result<FaceId, IndexFacesError> uploadImageToCollection(byte[] imageData, OffenderNumber offenderNumber, long imageId);

    /**
     * Find all matching faces in the collection that match the provided faceId
     */
    Set<FaceId> findMatchesFor(FaceId faceId);
}
