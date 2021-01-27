package uk.gov.justice.hmpps.datacompliance.config;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.List;
import java.util.Map;

import static com.amazonaws.services.sqs.model.QueueAttributeName.QueueArn;

@Configuration
@ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "embedded-localstack")
public class JmsEmbeddedLocalStackConfig {

    @Autowired
    LocalStackContainer localStackContainer;


    @Bean("dataComplianceRequestSqsClient")
    public AmazonSQS dataComplianceRequestSqsClient() {
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }

    @Bean("dataComplianceRequestSqsDlqClient")
    public AmazonSQS dataComplianceRequestSqsDlqClient() {
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }


    @Bean("dataComplianceResponseSqsClient")
    public AmazonSQS dataComplianceResponseSqsClient() {
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }

    @Bean("awsSnsClient")
    public AmazonSNS awsSnsClient() {
        return AmazonSNSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SNS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }

    @Bean("dataComplianceResponseSqsDlqClient")
    public AmazonSQS dataComplianceResponseSqsDlqClient() {
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }


    @Bean("sqsRequestQueueUrl")
    public String sqsRequestQueueUrl(@Qualifier("dataComplianceRequestSqsClient") AmazonSQS awsSqsClient, @Value("${data.compliance.request.sqs.queue.name}") final String queueName) {
        return awsSqsClient.getQueueUrl(queueName).getQueueUrl();
    }

    @Bean("sqsRequestDlqQueueUrl")
    public String sqsRequestDlqQueueUrl(@Qualifier("dataComplianceRequestSqsDlqClient") AmazonSQS awsSqsDlqClient, @Value("${data.compliance.request.sqs.dlq.name}") final String dlqQueueName) {
        return awsSqsDlqClient.getQueueUrl(dlqQueueName).getQueueUrl();
    }

    @Bean("sqsResponseQueueUrl")
    public String sqsResponseQueueUrl(@Qualifier("dataComplianceResponseSqsClient") AmazonSQS awsSqsClient, @Value("${data.compliance.response.sqs.queue.name}") final String queueName) {
        return awsSqsClient.getQueueUrl(queueName).getQueueUrl();
    }

    @Bean("sqsResponseDlqQueueUrl")
    public String sqsResponseDlqQueueUrl(@Qualifier("dataComplianceResponseSqsDlqClient") AmazonSQS awsSqsDlqClient, @Value("${data.compliance.response.sqs.dlq.name}") final String dlqQueueName) {
        return awsSqsDlqClient.getQueueUrl(dlqQueueName).getQueueUrl();
    }

    /*This is necessary due to a bug in localstack when running in test-containers that the ReDrive policy gets lost */
    private void dlqLocalStackHack(AmazonSQS awsSqsClient, String queueUrl, String dlqName) {
        var dlqUrl = awsSqsClient.getQueueUrl(dlqName).getQueueUrl();
        var dlqArn = awsSqsClient.getQueueAttributes(dlqUrl, List.of(QueueArn.toString()));
        final Map<String, String> attributes = Map.of(QueueAttributeName.RedrivePolicy.toString(),
            "{\"deadLetterTargetArn\":\"" + dlqArn.getAttributes().get("QueueArn") + "\",\"maxReceiveCount\":\"5\"}");

        awsSqsClient.createQueue(new CreateQueueRequest(queueUrl).withAttributes(attributes));
    }

}
