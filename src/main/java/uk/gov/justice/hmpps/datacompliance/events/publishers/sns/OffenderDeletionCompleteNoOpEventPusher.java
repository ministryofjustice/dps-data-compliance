package uk.gov.justice.hmpps.datacompliance.events.publishers.sns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete;

@Slf4j
@Component
@ConditionalOnExpression("!{'aws', 'localstack'}.contains('${hmpps.sqs.provider}')")
public class OffenderDeletionCompleteNoOpEventPusher implements OffenderDeletionCompleteEventPusher {

    public OffenderDeletionCompleteNoOpEventPusher() {
        log.info("Configured to ignore offender deletion complete events");
    }

    @Override
    public void sendEvent(final OffenderDeletionComplete event) {
        log.warn("Pretending to push offender deletion completed event for '{}' to queue", event.getOffenderIdDisplay());
    }
}
