package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.utils.Result;

import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.FACE_NOT_FOUND;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.error;

@Slf4j
@Component
@ConditionalOnProperty(value = "image.recognition.provider", matchIfMissing = true, havingValue = "no value set")
public class NoOpImageRecognitionClient implements ImageRecognitionClient {

    public NoOpImageRecognitionClient() {
        log.info("Configured to ignore image recognition requests");
    }

    @Override
    public Result<FaceId, IndexFacesError> uploadImageToCollection(final byte[] imageData,
                                                                   final OffenderNumber offenderNumber,
                                                                   final long imageId) {
        log.warn("Pretending to upload image data for offender: {}, image: {}", offenderNumber, imageId);
        return error(FACE_NOT_FOUND);
    }
}
