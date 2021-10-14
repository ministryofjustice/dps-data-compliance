package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.DestructionLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DestructionLogRepositoryTest extends IntegrationTest {


    @Autowired
    DestructionLogRepository destructionLogRepository;


    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldRetrieveOffenderDestructionLog() {
        final Page<DestructionLog> destructionLog = destructionLogRepository.findDestructionLog(Pageable.unpaged());


        assertThat(destructionLog).hasSize(2);
        assertThat(destructionLog).extracting(o -> o.getOffenderNumber().getOffenderNumber()).contains("C8841BD", "D8950VX");
        assertThat(destructionLog).extracting(DestructionLog::getFirstName).contains("Jake", "Lucy");
        assertThat(destructionLog).extracting(DestructionLog::getMiddleName).contains("Liam", "Lee");
        assertThat(destructionLog).extracting(DestructionLog::getLastName).contains("Oliver", "Rad");
        assertThat(destructionLog).extracting(DestructionLog::getLastKnownOmu).contains("LXH", "HMD");
        assertThat(destructionLog).extracting(DestructionLog::getMethodOfDestruction).contains("NOMIS database deletion", "NOMIS database deletion");
        assertThat(destructionLog).extracting(DestructionLog::getAuthorisationOfDestruction).contains("MOJ", "MOJ");
        assertThat(destructionLog).extracting(DestructionLog::getTypeOfRecordDestroyed).contains("NOMIS record", "NOMIS record");
        assertThat(destructionLog).extracting(DestructionLog::getDateOfBirth).contains((LocalDate.of(1998, 9, 2)), LocalDate.of(1987,10, 2));
        assertThat(destructionLog).extracting(DestructionLog::getDestructionDate).contains((LocalDateTime.of(2021,07,06,04,05,06)), LocalDateTime.of(2000,01,06,04,05,06));
    }

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void shouldRetrieveOffenderDestructionLogByDate() {
        final LocalDateTime destructionDate = LocalDateTime.of(2000,01,06,04,05,06);
        final Page<DestructionLog> destructionLog = destructionLogRepository.findDestructionLogBetweenDates(destructionDate.minusHours(1), destructionDate.plusHours(1), Pageable.unpaged());

        assertThat(destructionLog).hasSize(1);
        assertThat(destructionLog).extracting(o -> o.getOffenderNumber().getOffenderNumber()).containsOnly("D8950VX");
        assertThat(destructionLog).extracting(DestructionLog::getFirstName).containsOnly("Lucy");
        assertThat(destructionLog).extracting(DestructionLog::getMiddleName).containsOnly("Liam");
        assertThat(destructionLog).extracting(DestructionLog::getLastName).containsOnly("Oliver");
        assertThat(destructionLog).extracting(DestructionLog::getDateOfBirth).containsOnly(LocalDate.of(1987,10, 2));
        assertThat(destructionLog).extracting(DestructionLog::getLastKnownOmu).contains("HMD");
        assertThat(destructionLog).extracting(DestructionLog::getMethodOfDestruction).containsOnly("NOMIS database deletion");
        assertThat(destructionLog).extracting(DestructionLog::getAuthorisationOfDestruction).containsOnly("MOJ");
        assertThat(destructionLog).extracting(DestructionLog::getTypeOfRecordDestroyed).containsOnly("NOMIS record");
        assertThat(destructionLog).extracting(DestructionLog::getDestructionDate).contains(LocalDateTime.of(2000,01,06,04,05,06));
    }

}