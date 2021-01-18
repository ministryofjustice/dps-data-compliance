package uk.gov.justice.hmpps.datacompliance.config;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.localstack.LocalStackContainer;

@TestConfiguration
@ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "embedded-localstack")
public class JmsLocalStackConfig {

    @Autowired
    LocalStackContainer localStackContainer;


    @Bean()
    public AmazonSQS dataComplianceRequestSqsClient(){
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }

    @Bean
    public AmazonSQS dataComplianceRequestSqsDlqClient(){
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }


    @Bean
    public AmazonSQS dataComplianceResponseSqsClient(){
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }

    @Bean
    public AmazonSNS awsSnsClient(){
        return AmazonSNSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SNS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }

    @Bean()
    public AmazonSQS dataComplianceResponseSqsDlqClient(){
        return AmazonSQSClientBuilder
            .standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.getDefaultCredentialsProvider())
            .build();
    }






}
