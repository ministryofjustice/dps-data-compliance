package uk.gov.justice.hmpps.datacompliance.jobs.deceasedoffenderdeletion;

import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@RequiredArgsConstructor
public class DeceasedOffenderDeletionJob implements Job {

    private final DeceasedOffenderDeletion offenderDeletion;

    @Override
    public void execute(final JobExecutionContext context) {
        offenderDeletion.run();
    }
}

