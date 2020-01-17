package uk.gov.justice.hmpps.datacompliance.services.client.image.recognition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(value = "image.recognition.provider", matchIfMissing = true, havingValue = "no value set")
public class NoOpImageRecognitionClient implements ImageRecognitionClient {

    public NoOpImageRecognitionClient() {
        log.info("Configured to ignore image recognition requests");
    }

    @Override
    public Optional<String> uploadImageToCollection(final byte[] imageData, final String offenderNumber, final long imageId) {
        log.warn("Pretending to upload image data for offender: {}, image: {}", offenderNumber, imageId);
        return Optional.empty();
    }
}
