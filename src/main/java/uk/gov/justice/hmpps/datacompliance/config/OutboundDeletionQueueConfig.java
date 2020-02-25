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
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${outbound.deletion.sqs.provider}')")
public class OutboundDeletionQueueConfig {

    @Bean
    @ConditionalOnProperty(name = "outbound.deletion.sqs.provider", havingValue = "aws")
    public AmazonSQS outboundReferralSqsClient(
            @Value("${outbound.deletion.sqs.aws.access.key.id}") final String accessKey,
            @Value("${outbound.deletion.sqs.aws.secret.access.key}") final String secretKey,
            @Value("${outbound.deletion.sqs.region}") final String region) {

        log.info("Creating AWS outbound deletion SQS client");

        var credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "outbound.deletion.sqs.provider", havingValue = "aws")
    public AmazonSQS outboundReferralSqsDlqClient(
            @Value("${outbound.deletion.sqs.dlq.aws.access.key.id}") final String accessKey,
            @Value("${outbound.deletion.sqs.dlq.aws.secret.access.key}") final String secretKey,
            @Value("${outbound.deletion.sqs.region}") final String region) {

        log.info("Creating AWS outbound deletion SQS client for DLQ");

        var credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    @Bean("outboundDeletionSqsClient")
    @ConditionalOnProperty(name = "outbound.deletion.sqs.provider", havingValue = "localstack")
    public AmazonSQS sqsClientLocalstack(
            @Value("${outbound.deletion.sqs.endpoint.url}") final String serviceEndpoint,
            @Value("${outbound.deletion.sqs.region}") final String region) {

        log.info("Creating Localstack outbound deletion SQS client");

        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .build();
    }

    @Bean("outboundDeletionSqsDlqClient")
    @ConditionalOnProperty(name = "outbound.deletion.sqs.provider", havingValue = "localstack")
    public AmazonSQS sqsDlqClientLocalstack(
            @Value("${outbound.deletion.sqs.endpoint.url}") final String serviceEndpoint,
            @Value("${outbound.deletion.sqs.region}") final String region) {

        log.info("Creating Localstack outbound deletion SQS client for DLQ");

        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .build();
    }
}
