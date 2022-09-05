package uk.gov.justice.hmpps.datacompliance.jobs.deceasedoffenderdeletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.config.DeceasedOffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.dto.DeceasedOffenderDeletionRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch.BatchType;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.ManualRetentionRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(name = "deceased.offender.deletion.cron")
public class DeceasedOffenderDeletion {

    private final TimeSource timeSource;
    private final DeceasedOffenderDeletionConfig config;
    private final DeceasedOffenderDeletionBatchRepository repository;
    private final ManualRetentionRepository manualRetentionRepository;
    private final DataComplianceEventPusher eventPusher;

    public void run() {
        log.info("Running the 'deceased offender deletion' job");

        final var newBatch = persistNewBatch();
        final var request = buildRequest(newBatch);

        eventPusher.requestDeceasedOffenderDeletion(request);

        log.info("Deceased offender deletion request complete");

    }

    private DeceasedOffenderDeletionRequest buildRequest(DeceasedOffenderDeletionBatch newBatch) {
        final var request = DeceasedOffenderDeletionRequest.builder()
            .batchId(newBatch.getBatchId())
            .excludedOffenders(getRetainedOffenderNumbers());

        config.getDeletionLimit().ifPresent(request::limit);

        return request.build();
    }

    private Set<String> getRetainedOffenderNumbers() {
        return manualRetentionRepository.findAll().stream().map(ManualRetention::getOffenderNo).collect(Collectors.toSet());
    }

    private DeceasedOffenderDeletionBatch persistNewBatch() {

        log.info("Persisting new batch for deceased offenders due for deletion");

        return repository.save(DeceasedOffenderDeletionBatch.builder()
            .requestDateTime(timeSource.nowAsLocalDateTime())
            .batchType(BatchType.SCHEDULED)
            .build());
    }

}
