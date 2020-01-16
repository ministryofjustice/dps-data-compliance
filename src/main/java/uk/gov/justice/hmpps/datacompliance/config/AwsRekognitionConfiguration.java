package uk.gov.justice.hmpps.datacompliance.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsRekognitionConfiguration {

    @Bean
    @ConditionalOnProperty(name = "image.recognition.provider", havingValue = "aws")
    AmazonRekognition amazonRekognition(@Value("${image.recognition.aws.access.key.id}") String accessKey,
                                        @Value("${image.recognition.aws.secret.access.key}") String secretKey,
                                        @Value("${image.recognition.region}") String region) {

        final var credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonRekognitionClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}
