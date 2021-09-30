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
import uk.gov.justice.hmpps.datacompliance.jobs.deceasedoffenderdeletion.DeceasedOffenderDeletionJob;

import java.util.Optional;
import java.util.Set;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Slf4j
@Getter
@Configuration
@ConditionalOnProperty(name = "deceased.offender.deletion.cron")
public class DeceasedOffenderDeletionConfig {

    @Value("${deceased.offender.deletion.cron}")
    private String deceasedOffenderDeletionCron;

    @Value("${deceased.offender.deletion.limit:#{null}}")
    private Integer deletionLimit;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Bean
    public ApplicationRunner rescheduleDeceasedOffenderDeletion() {
        return args -> schedulerFactoryBean.getScheduler()
            .scheduleJob(
                deceasedOffenderDeletionJobDetails(),
                Set.of(deceasedOffenderDeletionTrigger()), true
            );
    }


    public Trigger deceasedOffenderDeletionTrigger() {

        log.info("Configured to delete deceased offenders with schedule: '{}'", deceasedOffenderDeletionCron);

        return TriggerBuilder.newTrigger().forJob(deceasedOffenderDeletionJobDetails())
            .withIdentity("deceased-offender-complete-deletion-trigger")
            .withSchedule(cronSchedule(deceasedOffenderDeletionCron))
            .build();
    }

    public JobDetail deceasedOffenderDeletionJobDetails() {
        return JobBuilder.newJob(DeceasedOffenderDeletionJob.class)
            .withIdentity("deceased-offender-complete-deletion-job")
            .storeDurably()
            .build();
    }

    public Optional<Integer> getDeletionLimit() {
        return Optional.ofNullable(deletionLimit);
    }

}
