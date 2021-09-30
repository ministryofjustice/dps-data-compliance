package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.ReferredDeceasedOffenderAlias;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DeceasedOffenderDeletionReferralRepositoryTest extends IntegrationTest {


    @Autowired
    private DeceasedOffenderDeletionBatchRepository batchRepository;

    @Autowired
    private DeceasedOffenderDeletionReferralRepository repository;

    @Test
    @Sql("classpath:seed.data/deceased_offender_deletion_batch.sql")
    @Sql("classpath:seed.data/deceased_offender_deletion_referral.sql")
    @Sql("classpath:seed.data/deceased_referred_offender_alias.sql")
    void getDeceasedOffenderDeletionReferral() {

        assertMatchesExpectedContents(repository.findById(1L).orElseThrow());
    }

    @Test
    @Sql("classpath:seed.data/deceased_offender_deletion_batch.sql")
    @Sql("classpath:seed.data/deceased_offender_deletion_referral.sql")
    @Sql("classpath:seed.data/deceased_referred_offender_alias.sql")
    void getDeceasedOffenderDeletionReferralByAgencyLocationId() {
        assertMatchesExpectedContents(repository.findByAgencyLocationId("LEI").get(0));
    }

    @Test
    @Sql("classpath:seed.data/deceased_offender_deletion_batch.sql")
    @Sql("classpath:seed.data/deceased_offender_deletion_referral.sql")
    void getDeceasedOffenderDeletionReferralWithoutBookingsOrResolution() {
        final var referral = repository.findById(1L).orElseThrow();

        assertThat(referral.getOffenderAliases()).isEmpty();
    }

    @Test
    @Sql("classpath:seed.data/deceased_offender_deletion_batch.sql")
    void saveDeceasedOffenderDeletionReferral() {

        final var referral = deceasedOffenderDeletionReferral();

        repository.save(referral);

        assertMatchesExpectedContents(repository.findById(referral.getReferralId()).orElseThrow());
    }


    private DeceasedOffenderDeletionReferral deceasedOffenderDeletionReferral() {
        final var referral = DeceasedOffenderDeletionReferral.builder()
                .deletionDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))
                .deceasedOffenderDeletionBatch(batchRepository.findById(1L).orElseThrow())
                .offenderNo("A1234AA")
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .agencyLocationId("LEI")
                .birthDate(LocalDate.of(1969, 1, 1))
                .deceasedDate(LocalDate.of(2000, 1, 1))
                .deletionDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))
                .build();

        referral.addReferredOffenderAlias(ReferredDeceasedOffenderAlias.builder()
                .offenderId(-1001L)
                .offenderBookId(-1L)
                .build());

        return referral;
    }



    private void assertMatchesExpectedContents(final DeceasedOffenderDeletionReferral referral) {
        assertThat(referral.getOffenderNo()).isEqualTo("A1234AA");
        assertThat(referral.getFirstName()).isEqualTo("John");
        assertThat(referral.getMiddleName()).isEqualTo("Middle");
        assertThat(referral.getLastName()).isEqualTo("Smith");
        assertThat(referral.getAgencyLocationId()).isEqualTo("LEI");
        assertThat(referral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(referral.getDeceasedDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(referral.getDeletionDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 2, 3, 4, 5));

        assertThat(referral.getOffenderAliases()).hasSize(1);
        assertMatchesExpectedContents(referral.getOffenderAliases().get(0));
    }

    private void assertMatchesExpectedContents(final ReferredDeceasedOffenderAlias offenderBooking) {
        assertThat(offenderBooking.getOffenderId()).isEqualTo(-1001L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(-1L);
    }



}
