package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.DataDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DataDuplicationDetectionService {

    private final TimeSource timeSource;
    private final DataComplianceEventPusher eventPusher;
    private final DataDuplicateRepository dataDuplicateRepository;

    public void searchForDuplicates(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to search for data duplicates: '{}/{}'",
                offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestDataDuplicateCheck(offenderNumber, retentionCheckId);
    }

    public List<DataDuplicate> persistDataDuplicates(final OffenderNumber referenceOffenderNo,
                                                     final List<OffenderNumber> duplicateOffenders) {

        final var timestamp = timeSource.nowAsLocalDateTime();

        return duplicateOffenders.stream()
                .map(duplicate -> dataDuplicateRepository.save(DataDuplicate.builder()
                        .detectionDateTime(timestamp)
                        .referenceOffenderNo(referenceOffenderNo.getOffenderNumber())
                        .duplicateOffenderNo(duplicate.getOffenderNumber())
                        .build()))
                .collect(toList());
    }
}
