package uk.gov.justice.hmpps.datacompliance.services.health;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${outbound.deletion.sqs.provider}')")
public class OutboundDeletionQueueHealth extends QueueHealth {

    public OutboundDeletionQueueHealth(
            @Autowired @Qualifier("outboundDeletionSqsClient") final AmazonSQS awsSqsClient,
            @Autowired @Qualifier("outboundDeletionSqsDlqClient") final AmazonSQS awsSqsDlqClient,
            @Value("${outbound.deletion.sqs.queue.name}") final String queueName,
            @Value("${outbound.deletion.sqs.dlq.name}") final String dlqName) {
        super(awsSqsClient, awsSqsDlqClient, queueName, dlqName);
    }
}
