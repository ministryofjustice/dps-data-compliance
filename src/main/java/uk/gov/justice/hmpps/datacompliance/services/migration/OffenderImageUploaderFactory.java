package uk.gov.justice.hmpps.datacompliance.services.migration;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Service
class OffenderImageUploaderFactory {

    private final Elite2ApiClient elite2ApiClient;
    private final ImageRecognitionClient imageRecognitionClient;
    private final OffenderImageUploadRepository repository;
    private final double uploadsPerSecond;

    OffenderImageUploaderFactory(@Value("${image.recognition.upload.permits.per.second:5.0}") final double uploadsPerSecond,
                                 final Elite2ApiClient elite2ApiClient,
                                 final ImageRecognitionClient imageRecognitionClient,
                                 final OffenderImageUploadRepository repository,
                                 final TimeSource timeSource) {

        this.elite2ApiClient = elite2ApiClient;
        this.imageRecognitionClient = imageRecognitionClient;
        this.repository = repository;
        this.timeSource = timeSource;
        this.uploadsPerSecond = uploadsPerSecond;
    }

    private final TimeSource timeSource;

    OffenderImageUploader generateUploaderFor(final ImageUploadBatch batch) {
        return new OffenderImageUploader(elite2ApiClient, imageRecognitionClient,
                new OffenderImageUploadLogger(repository, batch, timeSource),
                RateLimiter.create(uploadsPerSecond));
    }
}
