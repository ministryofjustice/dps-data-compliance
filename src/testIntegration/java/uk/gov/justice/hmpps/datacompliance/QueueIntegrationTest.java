package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.List;

import static org.awaitility.Awaitility.await;

public class QueueIntegrationTest extends IntegrationTest {

    public static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";


    @Value("${data.compliance.response.sqs.queue.name}")
    String sqsResponseQueueName;

    @Value("${data.compliance.request.sqs.queue.name}")
    String sqsRequestQueueName;

    @Value("${sns.topic.arn}")
    String snsTopicArn;

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


    int getNumberOfMessagesCurrentlyOnSqsQueue(String queueUrl, AmazonSQS client) {
        final GetQueueAttributesResult queueAttributes = client.getQueueAttributes(queueUrl, List.of(APPROXIMATE_NUMBER_OF_MESSAGES));
        return Integer.parseInt(queueAttributes.getAttributes().get(APPROXIMATE_NUMBER_OF_MESSAGES));
    }


    public void waitUntilRequestQueueMessagesAreConsumed() {
        await().until(() -> getNumberOfMessagesCurrentlyOnSqsQueue(sqsRequestClientQueueUrl, sqsRequestClient) == 0);
    }

    public int prisonApiRequestCountFor(String url) {
        //TODO: match url
        return prisonApiMock.getRequestCount();
    }

    public int pathFinderRequestForRequestCountFor(String url) {
        //TODO: match url
        return pathfinderApiMock.getRequestCount();
    }

    @Autowired
    @Qualifier("dataComplianceRequestSqsClient")
    AmazonSQS sqsRequestClient;

    @Autowired
    @Qualifier("dataComplianceRequestSqsDlqClient")
    AmazonSQS sqsRequestDlqClient;

    @Autowired
    @Qualifier("dataComplianceResponseSqsClient")
    AmazonSQS sqsResponseClient;

    @Autowired
    @Qualifier("dataComplianceResponseSqsDlqClient")
    AmazonSQS sqsResponseDlqClient;

    @Autowired
    AmazonSNS snsClient;

    @Autowired
    MockJmsListener mockJmsListener;

}
