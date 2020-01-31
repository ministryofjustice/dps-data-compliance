package uk.gov.justice.hmpps.datacompliance.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.justice.hmpps.datacompliance.services.migration.OffenderImageMigrationJob;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Slf4j
@Configuration
public class AwsRekognitionConfig {

    @Value("${image.recognition.migration.cron:@null}")
    private String imageMigrationCron;

    @Bean
    @ConditionalOnProperty(name = "image.recognition.provider", havingValue = "aws")
    AmazonRekognition amazonRekognition(@Value("${image.recognition.aws.access.key.id}") final String accessKey,
                                        @Value("${image.recognition.aws.secret.access.key}") final String secretKey,
                                        @Value("${image.recognition.region}") final String region) {

        final var credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonRekognitionClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "image.recognition.migration.cron")
    public Trigger offenderImageMigrationTrigger() {

        log.info("Configured to run offender image recognition migration with schedule: '{}'", imageMigrationCron);

        return TriggerBuilder.newTrigger().forJob(offenderImageMigrationJobDetails())
                .withIdentity("offender-image-migration-trigger")
                .withSchedule(cronSchedule(imageMigrationCron))
                .build();
    }

    @Bean
    @Lazy
    public JobDetail offenderImageMigrationJobDetails() {
        return JobBuilder.newJob(OffenderImageMigrationJob.class)
                .withIdentity("offender-image-migration-job")
                .storeDurably()
                .build();
    }
}
