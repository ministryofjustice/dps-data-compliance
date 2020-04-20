package uk.gov.justice.hmpps.datacompliance.jobs.offenderdeletion;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

public class OffenderDeletionJob implements Job {

    @Autowired
    private OffenderDeletion offenderDeletion;

    @Override
    public void execute(final JobExecutionContext context) {
        offenderDeletion.run();
    }
}
