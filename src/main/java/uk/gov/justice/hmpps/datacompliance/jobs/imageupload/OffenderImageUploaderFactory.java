package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Slf4j
@Service
class OffenderImageUploaderFactory {

    private final PrisonApiClient prisonApiClient;
    private final ImageRecognitionClient imageRecognitionClient;
    private final OffenderImageUploadRepository repository;
    private final double uploadsPerSecond;
    private final TimeSource timeSource;

    OffenderImageUploaderFactory(@Value("${image.recognition.upload.permits.per.second:5.0}") final double uploadsPerSecond,
                                 final PrisonApiClient prisonApiClient,
                                 final ImageRecognitionClient imageRecognitionClient,
                                 final OffenderImageUploadRepository repository,
                                 final TimeSource timeSource) {

        log.info("Image upload - rate limited to {} per second", uploadsPerSecond);

        this.prisonApiClient = prisonApiClient;
        this.imageRecognitionClient = imageRecognitionClient;
        this.repository = repository;
        this.timeSource = timeSource;
        this.uploadsPerSecond = uploadsPerSecond;
    }

    OffenderImageUploader generateUploaderFor(final ImageUploadBatch batch) {
        return new OffenderImageUploader(prisonApiClient, imageRecognitionClient,
            new OffenderImageUploadLogger(repository, batch, timeSource),
            RateLimiter.create(uploadsPerSecond));
    }
}
