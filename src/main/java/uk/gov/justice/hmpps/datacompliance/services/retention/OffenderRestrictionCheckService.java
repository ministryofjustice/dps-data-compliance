package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderRestrictionCode;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import javax.transaction.Transactional;
import java.util.Set;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class OffenderRestrictionCheckService {


    public static final String CHILD_RESTRICTION_COMMENT_REGEX = ".*(adopt|baby|boy([^f]|$)|child|chidl|chlid|chid|cild|hild|daughter|early age|girl([^f]|$)|infant|juven|kid|minor(\\W|$|s)|niece|nephew|(^|\\W)(grand)?son(\\W|$)|(^|\\W)teen|toddler|under( (the )?)?age|youth|young).*";

    private final DataComplianceEventPusher eventPusher;

    public void requestOffenderRestrictionCheck(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to perform an offender Restriction check: '{}/{}'",
            offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestOffenderRestrictionCheck(
            offenderNumber,
            retentionCheckId, Set.of(OffenderRestrictionCode.CHILD),
            CHILD_RESTRICTION_COMMENT_REGEX);
    }
}
