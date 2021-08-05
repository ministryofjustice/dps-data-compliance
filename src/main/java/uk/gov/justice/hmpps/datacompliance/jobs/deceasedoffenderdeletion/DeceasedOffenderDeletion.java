package uk.gov.justice.hmpps.datacompliance.jobs.deceasedoffenderdeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
@ConditionalOnProperty(name = "deceased.offender.deletion.cron")
public class DeceasedOffenderDeletion {

    void run() {
        log.info("Running the 'deceased offender deletion' job");
    }

}
