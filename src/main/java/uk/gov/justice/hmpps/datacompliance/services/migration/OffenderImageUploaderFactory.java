package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Service
@AllArgsConstructor
class OffenderImageUploaderFactory {

    private final Elite2ApiClient elite2ApiClient;
    private final ImageRecognitionClient imageRecognitionClient;
    private final OffenderImageUploadRepository repository;
    private final TimeSource timeSource;

    OffenderImageUploader generateUploaderFor(final ImageUploadBatch batch) {
        return new OffenderImageUploader(elite2ApiClient, imageRecognitionClient,
                new OffenderImageUploadLogger(repository, batch, timeSource));
    }
}
