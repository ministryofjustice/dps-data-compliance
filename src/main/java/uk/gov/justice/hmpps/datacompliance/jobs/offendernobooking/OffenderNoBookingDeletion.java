package uk.gov.justice.hmpps.datacompliance.jobs.offendernobooking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.config.OffenderNoBookingDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNoBookingDeletionRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.NoBookingDeletionRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionBatch.BatchType;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.offendernobooking.OffenderNoBookingDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(name = "offender.no.booking.deletion.cron")
public class OffenderNoBookingDeletion {

    private final TimeSource timeSource;
    private final OffenderNoBookingDeletionConfig config;
    private final OffenderNoBookingDeletionBatchRepository repository;
    private final DataComplianceEventPusher eventPusher;

    public void run() {
        log.info("Running the 'offender no booking deletion' job");

        final var newBatch = persistNewBatch();
        final var request = buildRequest(newBatch);

        eventPusher.requestOffenderNoBookingDeletion(request);

        log.info("Offender no booking deletion deletion request complete");

    }

    private OffenderNoBookingDeletionRequest buildRequest(OffenderNoBookingDeletionBatch newBatch) {
        final var request = OffenderNoBookingDeletionRequest.builder();
        request.batchId(newBatch.getBatchId());
        config.getDeletionLimit().ifPresent(request::limit);

        return request.build();
    }

    private OffenderNoBookingDeletionBatch persistNewBatch() {

        log.info("Persisting new batch for offender no booking due for deletion");

        return repository.save(OffenderNoBookingDeletionBatch.builder()
            .requestDateTime(timeSource.nowAsLocalDateTime())
            .batchType(BatchType.SCHEDULED)
            .build());
    }

}
