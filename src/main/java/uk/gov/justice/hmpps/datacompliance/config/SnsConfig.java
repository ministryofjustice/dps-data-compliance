package uk.gov.justice.hmpps.datacompliance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

import java.net.URI;

@Configuration
public class SnsConfig {

    @Bean
    @ConditionalOnProperty(name = "sns.provider", havingValue = "aws")
    SnsAsyncClient awsSnsClient(@Value("${sns.aws.access.key.id}") String accessKey,
                                @Value("${sns.aws.secret.access.key}") String secretKey,
                                @Value("${sns.endpoint.region}") String region) {
        return SnsAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "sns.provider", havingValue = "localstack")
    SnsAsyncClient awsLocalClient(@Value("${sns.endpoint.url}") String serviceEndpoint,
                                  @Value("${sns.endpoint.region}") String region) {
        return SnsAsyncClient.builder()
                .endpointOverride(URI.create(serviceEndpoint))
                .region(Region.of(region))
                .build();
    }
}
