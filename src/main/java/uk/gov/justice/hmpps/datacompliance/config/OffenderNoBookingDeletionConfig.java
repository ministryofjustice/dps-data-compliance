package uk.gov.justice.hmpps.datacompliance.config;

import lombok.Getter;
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
import uk.gov.justice.hmpps.datacompliance.jobs.offendernobooking.OffenderNoBookingDeletionJob;

import java.util.Optional;
import java.util.Set;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Slf4j
@Getter
@Configuration
@ConditionalOnProperty(name = "offender.no.booking.deletion.cron")
public class OffenderNoBookingDeletionConfig {

    @Value("${offender.no.booking.deletion.cron}")
    private String offenderNoBookingDeletionCron;

    @Value("${offender.no.booking.deletion.limit:#{null}}")
    private Integer deletionLimit;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Bean
    public ApplicationRunner rescheduleOffenderNoBookingDeletionDeletion() {
        return args -> schedulerFactoryBean.getScheduler()
            .scheduleJob(
                offenderNoBookingDeletionJobDetails(),
                Set.of(offenderNoBookingDeletionTrigger()), true
            );
    }


    public Trigger offenderNoBookingDeletionTrigger() {

        log.info("Configured to delete offenders with no booking with schedule: '{}'", offenderNoBookingDeletionCron);

        return TriggerBuilder.newTrigger().forJob(offenderNoBookingDeletionJobDetails())
            .withIdentity("offender-no-booking-deletion-trigger")
            .withSchedule(cronSchedule(offenderNoBookingDeletionCron))
            .build();
    }

    public JobDetail offenderNoBookingDeletionJobDetails() {
        return JobBuilder.newJob(OffenderNoBookingDeletionJob.class)
            .withIdentity("offender-no-booking-deletion-job")
            .storeDurably()
            .build();
    }

    public Optional<Integer> getDeletionLimit() {
        return Optional.ofNullable(deletionLimit);
    }

}
