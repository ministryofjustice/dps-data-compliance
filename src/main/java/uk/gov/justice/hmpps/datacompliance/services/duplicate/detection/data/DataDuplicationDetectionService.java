package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.duplicate.detection.DuplicateDetectionClient;
import uk.gov.justice.hmpps.datacompliance.dto.DuplicateResult;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.DataDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ANALYTICAL_PLATFORM;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DataDuplicationDetectionService {

    private final TimeSource timeSource;
    private final DataComplianceEventPusher eventPusher;
    private final DataDuplicateRepository dataDuplicateRepository;
    private final DuplicateDetectionClient duplicateDetectionClient;

    public void searchForIdDuplicates(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to search for data duplicates by ID: '{}/{}'",
                offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestIdDataDuplicateCheck(offenderNumber, retentionCheckId);
    }

    public void searchForDatabaseDuplicates(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to search for data duplicates (using similarity query on NOMIS DB): '{}/{}'",
                offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestDatabaseDataDuplicateCheck(offenderNumber, retentionCheckId);
    }

    public List<DataDuplicate> searchForAnalyticalPlatformDuplicates(final OffenderNumber offenderNumber) {

        log.debug("Performing Analytical Platform duplicate search for offender : '{}'",
                offenderNumber.getOffenderNumber());

        final var duplicates = duplicateDetectionClient.findDuplicatesFor(offenderNumber);

        return persistDataDuplicates(offenderNumber, duplicates, ANALYTICAL_PLATFORM);
    }

    public List<DataDuplicate> persistDataDuplicates(final OffenderNumber referenceOffenderNo,
                                                     final Collection<DuplicateResult> duplicates,
                                                     final Method method) {

        final var timestamp = timeSource.nowAsLocalDateTime();

        return duplicates.stream()
                .map(duplicate -> dataDuplicateRepository.save(DataDuplicate.builder()
                        .detectionDateTime(timestamp)
                        .referenceOffenderNo(referenceOffenderNo.getOffenderNumber())
                        .duplicateOffenderNo(duplicate.getDuplicateOffenderNumber().getOffenderNumber())
                        .confidence(duplicate.getConfidence())
                        .method(method)
                        .build()))
                .collect(toList());
    }
}
