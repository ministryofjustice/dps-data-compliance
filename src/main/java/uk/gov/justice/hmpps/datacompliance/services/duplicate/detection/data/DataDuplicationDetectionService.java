package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

@Slf4j
@Service
@AllArgsConstructor
public class DataDuplicationDetectionService {

    private final DataComplianceEventPusher eventPusher;

    public void searchForDuplicates(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to search for data duplicates: '{}/{}'",
                offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestDataDuplicateCheck(offenderNumber, retentionCheckId);
    }
}
