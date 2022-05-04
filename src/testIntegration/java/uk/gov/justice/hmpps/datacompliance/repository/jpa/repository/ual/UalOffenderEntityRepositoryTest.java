package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UalOffenderEntityRepositoryTest extends IntegrationTest {

    @Autowired
    private OffenderUalRepository offenderUalRepository;

    @Test
    @Sql("classpath:seed.data/offender_ual.sql")
    public void findOneByOffenderNo() {
        assertThat(offenderUalRepository.findOneByOffenderNoIgnoreCase("A1234AA")).isPresent();
        assertThat(offenderUalRepository.findOneByOffenderNoIgnoreCase("A1234AA").get().getOffenderUalId()).isEqualTo(1L);
    }

    @Test
    @Sql("classpath:seed.data/offender_ual.sql")
    public void findOneByOffenderBookingNo() {
        final Optional<OffenderUalEntity> offender = offenderUalRepository.findOneByOffenderBookingNoIgnoreCase("PR2788");
        final Optional<OffenderUalEntity> offender2 = offenderUalRepository.findOneByOffenderBookingNoIgnoreCase("CR2799");

        assertThat(offender).isPresent();
        assertThat(offender.get().getOffenderUalId()).isEqualTo(1L);

        assertThat(offender2).isPresent();
        assertThat(offender2.get().getOffenderUalId()).isEqualTo(2L);
    }

    @Sql("classpath:seed.data/offender_ual.sql")
    public void findByOffenderPnc() {
        final Optional<OffenderUalEntity> offender = offenderUalRepository.findOneByOffenderPncIgnoreCase("13862/77U'");

        assertThat(offender).isPresent();
        assertThat(offender.get().getOffenderUalId()).isEqualTo(1L);
    }

}
