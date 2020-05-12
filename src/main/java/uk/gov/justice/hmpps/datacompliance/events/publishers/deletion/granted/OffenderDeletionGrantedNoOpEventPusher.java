package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

@Slf4j
@Component
@ConditionalOnExpression("!{'aws', 'localstack'}.contains('${data.compliance.request.sqs.provider}')")
public class OffenderDeletionGrantedNoOpEventPusher implements OffenderDeletionGrantedEventPusher {

    public OffenderDeletionGrantedNoOpEventPusher() {
        log.info("Configured to ignore offender deletion granted events");
    }

    @Override
    public void grantDeletion(final OffenderNumber offenderNo, final Long referralId) {
        log.warn("Pretending to push offender deletion granted events for '{}/{}' to queue",
                offenderNo.getOffenderNumber(), referralId);
    }
}
