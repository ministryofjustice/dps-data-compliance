package uk.gov.justice.hmpps.datacompliance.services.events.publishers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("!{'aws', 'localstack'}.contains('${outbound.deletion.sqs.provider}')")
public class OffenderDeletionGrantedNoOpEventPusher implements OffenderDeletionGrantedEventPusher {

    public OffenderDeletionGrantedNoOpEventPusher() {
        log.info("Configured to ignore offender deletion granted events");
    }

    @Override
    public void sendEvent(final String offenderIdDisplay) {
        log.warn("Pretending to push offender deletion granted events for '{}' to queue", offenderIdDisplay);
    }
}
