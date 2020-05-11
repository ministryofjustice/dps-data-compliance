package uk.gov.justice.hmpps.datacompliance.config;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import uk.gov.justice.hmpps.datacompliance.jobs.imageupload.OffenderImageMigrationJob;

import java.util.Set;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Slf4j
@Configuration
public class AwsRekognitionConfig {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Bean
    @ConditionalOnProperty(name = "image.recognition.provider", havingValue = "aws")
    RekognitionClient amazonRekognition(@Value("${image.recognition.aws.access.key.id}") final String accessKey,
                                        @Value("${image.recognition.aws.secret.access.key}") final String secretKey,
                                        @Value("${image.recognition.region}") final String region) {

        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return RekognitionClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "image.recognition.migration.cron")
    public ApplicationRunner rescheduleImageMigration(@Value("${image.recognition.migration.cron}") final String imageMigrationCron) {
        return args -> schedulerFactoryBean.getScheduler()
                .scheduleJob(
                        offenderImageMigrationJobDetails(),
                        Set.of(offenderImageMigrationTrigger(imageMigrationCron)),
                        true);
    }

    public Trigger offenderImageMigrationTrigger(final String imageMigrationCron) {

        log.info("Configured to run offender image recognition migration with schedule: '{}'", imageMigrationCron);

        return TriggerBuilder.newTrigger().forJob(offenderImageMigrationJobDetails())
                .withIdentity("offender-image-migration-trigger")
                .withSchedule(cronSchedule(imageMigrationCron))
                .build();
    }

    public JobDetail offenderImageMigrationJobDetails() {
        return JobBuilder.newJob(OffenderImageMigrationJob.class)
                .withIdentity("offender-image-migration-job")
                .storeDurably()
                .build();
    }
}
