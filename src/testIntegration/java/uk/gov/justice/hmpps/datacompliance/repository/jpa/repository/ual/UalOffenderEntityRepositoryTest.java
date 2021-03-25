package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UalOffenderEntityRepositoryTest extends IntegrationTest {

    @Autowired
    private OffenderUalRepository offenderUalRepository;

    @Test
    @Sql("classpath:seed.data/offender_ual.sql")
    public void findOneByOffenderNo() {
        assertThat(offenderUalRepository.findOneByOffenderNo("A1234AA")).isPresent();
        assertThat(offenderUalRepository.findOneByOffenderNo("A1234AA").get().getOffenderUalId()).isEqualTo(1L);
    }

    @Test
    @Sql("classpath:seed.data/offender_ual.sql")
    public void findOneByOffenderBookingNoAndOffenderCroPncAndFirstNamesAndLastName() {
        final Optional<OffenderUalEntity> offender = offenderUalRepository.findOneByOffenderBookingNoAndOffenderCroPncAndFirstNamesAndLastName("PR2788", "13862/77U", "John", "Smith");
        assertThat(offender).isPresent();
        assertThat(offender.get().getOffenderUalId()).isEqualTo(1L);
    }

    @Test
    @Sql("classpath:seed.data/offender_ual.sql")
    public void deleteByOffenderUalIdNotIn() {
        assertThat(StreamSupport.stream(offenderUalRepository.findAll().spliterator(), false).count()).isEqualTo(2);

        offenderUalRepository.deleteByOffenderUalIdNotIn(List.of(1L));

        assertThat(StreamSupport.stream(offenderUalRepository.findAll().spliterator(), false).count()).isEqualTo(1);
        assertThat(offenderUalRepository.findById(1L)).isPresent();
    }

}
