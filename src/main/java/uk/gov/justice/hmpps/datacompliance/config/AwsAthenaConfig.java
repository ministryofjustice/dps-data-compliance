package uk.gov.justice.hmpps.datacompliance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;


@Slf4j
@Configuration
public class AwsAthenaConfig {

    private static final String SESSION_NAME = "PrisonDataComplianceDuplicateSearch";

    @Bean
    @ConditionalOnProperty(name = "duplicate.detection.provider", havingValue = "aws")
    AthenaClient athenaClient(@Value("${duplicate.detection.aws.access.key.id}") final String accessKey,
                              @Value("${duplicate.detection.aws.secret.access.key}") final String secretKey,
                              @Value("${duplicate.detection.region}") final String region,
                              @Value("${duplicate.detection.role.arn}") final String roleArn) {
        return AthenaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(assumedRoleCredentialsProvider(accessKey, secretKey, region, roleArn))
                .build();
    }

    private AwsCredentialsProvider assumedRoleCredentialsProvider(final String accessKey,
                                                                  final String secretKey,
                                                                  final String region,
                                                                  final String roleArn) {

        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);

        final var assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .roleSessionName(SESSION_NAME)
                .build();

        final var stsClient = StsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(assumeRoleRequest)
                .asyncCredentialUpdateEnabled(true)
                .build();
    }
}
