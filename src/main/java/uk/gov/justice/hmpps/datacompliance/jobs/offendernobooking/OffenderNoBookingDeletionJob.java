package uk.gov.justice.hmpps.datacompliance.jobs.offendernobooking;

import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@RequiredArgsConstructor
public class OffenderNoBookingDeletionJob implements Job {

    private final OffenderNoBookingDeletion offenderDeletion;

    @Override
    public void execute(final JobExecutionContext context) {
        offenderDeletion.run();
    }
}

