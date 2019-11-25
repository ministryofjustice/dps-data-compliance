package uk.gov.justice.hmpps.datacompliance.services.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "sns.provider", matchIfMissing = true, havingValue = "no value set")
public class OffenderDeletionNoOpEventPusher implements OffenderDeletionEventPusher {

    public OffenderDeletionNoOpEventPusher() {
        log.info("Configured to ignore offender deletion events");
    }

    @Override
    public void sendEvent(final String offenderIdDisplay) {
        log.warn("Pretending to push offender deletion for {} to event topic", offenderIdDisplay);
    }
}
