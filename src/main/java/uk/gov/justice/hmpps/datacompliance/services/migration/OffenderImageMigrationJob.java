package uk.gov.justice.hmpps.datacompliance.services.migration;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

public class OffenderImageMigrationJob implements Job {

    @Autowired
    private OffenderImageMigration migration;

    @Override
    public void execute(final JobExecutionContext context) {
        migration.run();
    }
}
