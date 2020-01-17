package uk.gov.justice.hmpps.datacompliance.services.client.image.recognition;

import java.util.Optional;

public interface ImageRecognitionClient {

    /**
     * Upload image and index face into collection for facial recogni.
     * @return Optional server-side unique identifier for the face recognised in the image. Empty if no face detected.
     */
    Optional<String> uploadImageToCollection(byte[] imageData, final String offenderNumber, final long imageId);

}
