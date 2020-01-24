package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.services.migration.OffenderIterator.OffenderAction;

@Slf4j
@Service
@AllArgsConstructor
public class OffenderImageUploader implements OffenderAction {
    
    private final Elite2ApiClient elite2ApiClient;
    private final ImageRecognitionClient imageRecognitionClient;

    @Override
    public void accept(final OffenderNumber offenderNumber) {

        log.debug("Uploading image data for offender: '{}'", offenderNumber.getOffenderNumber());

        var faceImages = elite2ApiClient.getOffenderFaceImagesFor(offenderNumber);

        if (faceImages.isEmpty()) {
            log.debug("Offender: '{}' has no face images to upload", offenderNumber);
            return;
        }

        faceImages.forEach(image -> {

            log.trace("Uploading image: '{}' for offender: '{}'", image.getImageId(), offenderNumber.getOffenderNumber());

            byte[] imageData = elite2ApiClient.getImageData(image.getImageId());

            imageRecognitionClient.uploadImageToCollection(imageData, offenderNumber, image.getImageId());
        });
    }
}
