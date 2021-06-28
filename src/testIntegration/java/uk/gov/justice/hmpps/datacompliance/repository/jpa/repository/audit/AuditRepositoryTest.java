package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.DestructionLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRepositoryTest extends IntegrationTest {


    @Autowired
    AuditRepository auditRepository;


    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    @Sql("classpath:seed.data/image_duplicate.sql")
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    @Sql("classpath:seed.data/retention_reason_image_duplicate.sql")
    void shouldRetrieveOffenderDestructionLog() {
        final List<DestructionLog> destructionLog = auditRepository.findDestructionLog();


        assertThat(destructionLog).hasSize(2);
        assertThat(destructionLog).extracting(o -> o.getOffenderNumber().getOffenderNumber()).contains("A1234AA", "C8841BD");
        assertThat(destructionLog).extracting(DestructionLog::getFirstName).contains("John", "Jake");
        assertThat(destructionLog).extracting(DestructionLog::getMiddleName).contains("Middle", "Lee");
        assertThat(destructionLog).extracting(DestructionLog::getLastName).contains("Smith", "Rad");
        assertThat(destructionLog).extracting(DestructionLog::getMethodOfDestruction).contains("NOMIS database deletion", "NOMIS database deletion");
        assertThat(destructionLog).extracting(DestructionLog::getAuthorisationOfDestruction).contains("MOJ", "MOJ");
        assertThat(destructionLog).extracting(DestructionLog::getTypeOfRecordDestroyed).contains("NOMIS record", "NOMIS record");
        assertThat(destructionLog).extracting(DestructionLog::getDateOfBirth).contains((LocalDate.of(1969, 01, 01)), LocalDate.of(1969, 01, 01));
        assertThat(destructionLog).extracting(DestructionLog::getDestructionDate).contains((LocalDateTime.of(2021, 02, 03, 04, 05, 06)), LocalDateTime.of(2021, 02, 03, 04, 05, 06));
    }

    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    @Sql("classpath:seed.data/image_duplicate.sql")
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    @Sql("classpath:seed.data/retention_reason_image_duplicate.sql")
    void shouldRetrieveOffenderDestructionLogByDate() {
        final LocalDateTime destructionDate = LocalDateTime.of(2021, 02, 03, 04, 05, 06);
        final List<DestructionLog> destructionLog = auditRepository.findDestructionLogBetweenDates(destructionDate.minusHours(1), destructionDate.plusHours(1));

        assertThat(destructionLog).hasSize(1);
        assertThat(destructionLog).extracting(o -> o.getOffenderNumber().getOffenderNumber()).contains("A1234AA");
        assertThat(destructionLog).extracting(DestructionLog::getFirstName).contains("John");
        assertThat(destructionLog).extracting(DestructionLog::getMiddleName).contains("Middle");
        assertThat(destructionLog).extracting(DestructionLog::getLastName).contains("Smith");
        assertThat(destructionLog).extracting(DestructionLog::getMethodOfDestruction).contains("NOMIS database deletion");
        assertThat(destructionLog).extracting(DestructionLog::getAuthorisationOfDestruction).contains("MOJ");
        assertThat(destructionLog).extracting(DestructionLog::getTypeOfRecordDestroyed).contains("NOMIS record");
        assertThat(destructionLog).extracting(DestructionLog::getDateOfBirth).contains((LocalDate.of(1969, 01, 01)));
        assertThat(destructionLog).extracting(DestructionLog::getDestructionDate).contains(destructionDate);
    }

}