package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import javax.transaction.Transactional;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MoratoriumCheckService {

    // TODO GDPR-57 Implement RegEx
    public static final String MORATORIUM_REGEX = "GDPR-57";

    private final DataComplianceEventPusher eventPusher;

    public void requestFreeTextSearch(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to perform free text search for offender: '{}/{}'",
                offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestFreeTextMoratoriumCheck(offenderNumber, retentionCheckId, MORATORIUM_REGEX);
    }
}
