package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.RetainedOffender;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RetainedOffenderRepositoryTest extends JpaRepositoryTest {


    @Autowired
    RetainedOffenderRepository repository;

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check_3.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void findRetainedOffenders() {

        final Page<RetainedOffender> retainedOffenders = repository.findRetainedOffenders(Pageable.unpaged());

        assertThat(retainedOffenders).hasSize(1);
        assertThat(retainedOffenders).extracting(o -> o.getOffenderNumber().getOffenderNumber()).containsOnly("A1234AA");
        assertThat(retainedOffenders).extracting(RetainedOffender::getFirstName).containsOnly("John");
        assertThat(retainedOffenders).extracting(RetainedOffender::getMiddleName).containsOnly("Middle");
        assertThat(retainedOffenders).extracting(RetainedOffender::getLastName).containsOnly("Smith");
        assertThat(retainedOffenders).extracting(RetainedOffender::getLastKnownOmu).containsOnly("LEI");
        assertThat(retainedOffenders).extracting(RetainedOffender::getDateOfBirth).contains(LocalDate.of(1969, 1, 1));
        assertThat(retainedOffenders).extracting(RetainedOffender::getPositiveRetentionChecks).contains(List.of("ALERT", "DATA_DUPLICATE_AP", "DATA_DUPLICATE_ID", "IMAGE_DUPLICATE"));

    }

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_alias.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/retention_check_3.sql")
    @Sql("classpath:seed.data/retention_reason_manual.sql")
    void findRetainedOffenderDuplicates() {

        final Page<RetainedOffender> retainedOffenders = repository.findRetainedOffenderDuplicates(Pageable.unpaged());

        assertThat(retainedOffenders).hasSize(1);
        assertThat(retainedOffenders).extracting(o -> o.getOffenderNumber().getOffenderNumber()).containsOnly("A1234AA");
        assertThat(retainedOffenders).extracting(RetainedOffender::getFirstName).containsOnly("John");
        assertThat(retainedOffenders).extracting(RetainedOffender::getMiddleName).containsOnly("Middle");
        assertThat(retainedOffenders).extracting(RetainedOffender::getLastName).containsOnly("Smith");
        assertThat(retainedOffenders).extracting(RetainedOffender::getLastKnownOmu).containsOnly("LEI");
        assertThat(retainedOffenders).extracting(RetainedOffender::getDateOfBirth).contains(LocalDate.of(1969, 1, 1));
        assertThat(retainedOffenders).extracting(RetainedOffender::getPositiveRetentionChecks).contains(List.of("DATA_DUPLICATE_AP", "DATA_DUPLICATE_ID", "IMAGE_DUPLICATE"));

    }
}
