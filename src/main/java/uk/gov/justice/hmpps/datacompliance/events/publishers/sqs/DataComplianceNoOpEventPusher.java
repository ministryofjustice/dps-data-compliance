package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.List;

@Slf4j
@Component
@ConditionalOnExpression("!{'aws', 'localstack'}.contains('${data.compliance.request.sqs.provider}')")
public class DataComplianceNoOpEventPusher implements DataComplianceEventPusher {

    public DataComplianceNoOpEventPusher() {
        log.info("Configured to ignore offender deletion granted events");
    }

    @Override
    public void grantDeletion(final OffenderDeletionGrant offenderDeletionGrant) {
        log.warn("Pretending to push offender deletion granted events for '{}/{}' to queue",
                offenderDeletionGrant.getOffenderNumber().getOffenderNumber(), offenderDeletionGrant.getReferralId());
    }

    @Override
    public void requestIdDataDuplicateCheck(final OffenderNumber offenderNo, final Long retentionCheckId) {
        log.warn("Pretending to push ID data duplicate check '{}/{}' to queue",
                offenderNo.getOffenderNumber(), retentionCheckId);
    }

    @Override
    public void requestDatabaseDataDuplicateCheck(final OffenderNumber offenderNo, final Long retentionCheckId) {
        log.warn("Pretending to push data duplicate database check '{}/{}' to queue",
                offenderNo.getOffenderNumber(), retentionCheckId);
    }

    @Override
    public void requestFreeTextMoratoriumCheck(final OffenderNumber offenderNo,
                                               final Long retentionCheckId,
                                               final List<String> regex) {
        log.warn("Pretending to push free text moratorium check '{}/{}' to queue",
                offenderNo.getOffenderNumber(), retentionCheckId);
    }
}
