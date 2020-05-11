package uk.gov.justice.hmpps.datacompliance.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;

@Slf4j
@EnableJms
@Configuration
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${data.compliance.request.sqs.provider}')")
public class DataComplianceRequestQueueConfig {

    @Bean
    @ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "aws")
    public AmazonSQS dataComplianceRequestSqsClient(
            @Value("${data.compliance.request.sqs.aws.access.key.id}") final String accessKey,
            @Value("${data.compliance.request.sqs.aws.secret.access.key}") final String secretKey,
            @Value("${data.compliance.request.sqs.region}") final String region) {

        log.info("Creating AWS data compliance request SQS client");

        var credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "aws")
    public AmazonSQS dataComplianceRequestSqsDlqClient(
            @Value("${data.compliance.request.sqs.dlq.aws.access.key.id}") final String accessKey,
            @Value("${data.compliance.request.sqs.dlq.aws.secret.access.key}") final String secretKey,
            @Value("${data.compliance.request.sqs.region}") final String region) {

        log.info("Creating AWS data compliance request SQS client for DLQ");

        var credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    @Bean("dataComplianceRequestSqsClient")
    @ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "localstack")
    public AmazonSQS sqsClientLocalstack(
            @Value("${data.compliance.request.sqs.endpoint.url}") final String serviceEndpoint,
            @Value("${data.compliance.request.sqs.region}") final String region) {

        log.info("Creating Localstack data compliance request SQS client");

        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .build();
    }

    @Bean("dataComplianceRequestSqsDlqClient")
    @ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "localstack")
    public AmazonSQS sqsDlqClientLocalstack(
            @Value("${data.compliance.request.sqs.endpoint.url}") final String serviceEndpoint,
            @Value("${data.compliance.request.sqs.region}") final String region) {

        log.info("Creating Localstack data compliance request SQS client for DLQ");

        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .build();
    }
}
