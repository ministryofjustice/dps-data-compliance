package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import junit.framework.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static org.awaitility.Awaitility.await;

public class QueueIntegrationTest extends IntegrationTest {

    public static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";

    public void waitForPathFinderApiRequestTo(String url) {
        await().until(() -> requestExists(url));
    }

    public void waitUntilResponseQueueMessagesAreConsumed() {
        await().until(() -> getNumberOfMessagesCurrentlyOnSqsQueue(sqsResponseClientQueueUrl, sqsRequestClient) == 0);
    }

    int getNumberOfMessagesCurrentlyOnSqsQueue(String queueUrl, AmazonSQS client) {
        final GetQueueAttributesResult queueAttributes = client.getQueueAttributes(queueUrl, List.of(APPROXIMATE_NUMBER_OF_MESSAGES));
        return Integer.parseInt(queueAttributes.getAttributes().get(APPROXIMATE_NUMBER_OF_MESSAGES));
    }


    public boolean requestExists(String url) {
        try {
            return pathfinderApiMock.takeRequest().getRequestUrl().toString().contains(url);
        } catch (InterruptedException e) {
            throw new AssertionFailedError();
        }
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

    @Value("${data.compliance.response.sqs.queue.name}")
    String sqsResponseQueueName;

    @Value("${data.compliance.request.sqs.queue.name}")
    String sqsRequestQueueName;

    @Autowired
    @Qualifier("sqsRequestQueueUrl")
    public String sqsRequestClientQueueUrl;

    @Autowired
    @Qualifier("sqsRequestDlqQueueUrl")
    public String sqsRequestDlqClientQueueUrl;

    @Autowired
    @Qualifier("sqsResponseQueueUrl")
    public String sqsResponseClientQueueUrl;

    @Autowired
    @Qualifier("sqsResponseDlqQueueUrl")
    public String sqsResponseDlqClientQueueUrl;

    @Autowired
    MockJmsListener mockJmsListener;


}
