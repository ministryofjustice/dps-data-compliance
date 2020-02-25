package uk.gov.justice.hmpps.datacompliance.services.health;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${inbound.referral.sqs.provider}')")
public class InboundReferralQueueHealth extends QueueHealth {

    public InboundReferralQueueHealth(
            @Autowired @Qualifier("inboundReferralSqsClient") final AmazonSQS awsSqsClient,
            @Autowired @Qualifier("inboundReferralSqsDlqClient") final AmazonSQS awsSqsDlqClient,
            @Value("${inbound.referral.sqs.queue.name}") final String queueName,
            @Value("${inbound.referral.sqs.dlq.name}") final String dlqName) {
        super(awsSqsClient, awsSqsDlqClient, queueName, dlqName);
    }
}
