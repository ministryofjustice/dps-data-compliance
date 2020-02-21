package uk.gov.justice.hmpps.datacompliance.services.client.image.recognition;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.utils.Result;

import java.util.Optional;

public interface ImageRecognitionClient {

    /**
     * Upload image and index face into collection for facial recognition.
     * @return Optional server-side unique identifier for the face recognised in the image. Empty if no face detected.
     */
    Result<FaceId, IndexFacesError> uploadImageToCollection(byte[] imageData, final OffenderNumber offenderNumber, final long imageId);

}
