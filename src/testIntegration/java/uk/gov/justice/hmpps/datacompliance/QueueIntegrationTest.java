package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import junit.framework.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static org.awaitility.Awaitility.await;

public class QueueIntegrationTest extends IntegrationTest {

    public void waitForPathFinderApiRequestTo(String url) {
        await().until(() -> requestExists(url));
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
