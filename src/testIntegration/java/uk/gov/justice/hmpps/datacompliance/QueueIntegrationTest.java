package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.List;

public class QueueIntegrationTest extends IntegrationTest {

    public static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";


    @Value("${data.compliance.response.sqs.queue.name}")
    String sqsResponseQueueName;

    @Value("${data.compliance.request.sqs.queue.name}")
    String sqsRequestQueueName;

    @Value("${sns.topic.arn}")
    String snsQueueName;


    String sqsRequestClientQueueUrl;
    String sqsRequestDlqClientQueueUrl;
    String sqsResponseClientQueueUrl;
    String sqsResponseDlqClientQueueUrl;

    @PostConstruct
    public void initialiseQueueUrls() {
        sqsRequestClientQueueUrl = sqsRequestClient.getQueueUrl(sqsRequestQueueName).getQueueUrl();
        sqsRequestDlqClientQueueUrl = sqsRequestDlqClient.getQueueUrl(sqsRequestQueueName).getQueueUrl();
        sqsResponseClientQueueUrl = sqsResponseClient.getQueueUrl(sqsResponseQueueName).getQueueUrl();
        sqsResponseDlqClientQueueUrl = sqsResponseDlqClient.getQueueUrl(sqsResponseQueueName).getQueueUrl();
    }


    int getNumberOfMessagesCurrentlyOnQueue(String queueUrl, AmazonSQS client) {
        final GetQueueAttributesResult queueAttributes = client.getQueueAttributes(queueUrl, List.of(APPROXIMATE_NUMBER_OF_MESSAGES));
        return Integer.parseInt(queueAttributes.getAttributes().get(APPROXIMATE_NUMBER_OF_MESSAGES));
    }
}
