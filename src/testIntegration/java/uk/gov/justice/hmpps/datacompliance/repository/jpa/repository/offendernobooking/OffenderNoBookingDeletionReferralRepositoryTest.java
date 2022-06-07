package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.offendernobooking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.ReferredOffenderNoBookingAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OffenderNoBookingDeletionReferralRepositoryTest extends JpaRepositoryTest {


    @Autowired
    private OffenderNoBookingDeletionBatchRepository batchRepository;

    @Autowired
    private OffenderNoBookingDeletionReferralRepository repository;

    @Test
    @Sql("classpath:seed.data/offender_no_booking_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_no_booking_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_no_booking_alias.sql")
    void getDeceasedOffenderDeletionReferral() {

        assertMatchesExpectedContents(repository.findById(1L).orElseThrow());
    }

    @Test
    @Sql("classpath:seed.data/offender_no_booking_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_no_booking_deletion_referral.sql")
    @Sql("classpath:seed.data/referred_offender_no_booking_alias.sql")
    void getDeceasedOffenderDeletionReferralByAgencyLocationId() {
        assertMatchesExpectedContents(repository.findByAgencyLocationId("LEI").get(0));
    }

    @Test
    @Sql("classpath:seed.data/offender_no_booking_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_no_booking_deletion_referral.sql")
    void getDeceasedOffenderDeletionReferralWithoutBookingsOrResolution() {
        final var referral = repository.findById(1L).orElseThrow();

        assertThat(referral.getOffenderAliases()).isEmpty();
    }

    @Test
    @Sql("classpath:seed.data/offender_no_booking_deletion_batch.sql")
    void saveDeceasedOffenderDeletionReferral() {

        final var referral = offenderNoBookingDeletionReferral();

        repository.save(referral);

        assertMatchesExpectedContents(repository.findById(referral.getReferralId()).orElseThrow());
    }


    private OffenderNoBookingDeletionReferral offenderNoBookingDeletionReferral() {
        final var referral = OffenderNoBookingDeletionReferral.builder()
            .deletionDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))
            .offenderNoBookingDeletionBatch(batchRepository.findById(1L).orElseThrow())
            .offenderNo("A1234AA")
            .firstName("John")
            .middleName("Middle")
            .lastName("Smith")
            .agencyLocationId("LEI")
            .birthDate(LocalDate.of(1969, 1, 1))
            .deletionDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))
            .build();

        referral.addReferredOffenderAlias(ReferredOffenderNoBookingAlias.builder()
            .offenderId(-1001L)
            .build());

        return referral;
    }


    private void assertMatchesExpectedContents(final OffenderNoBookingDeletionReferral referral) {
        assertThat(referral.getOffenderNo()).isEqualTo("A1234AA");
        assertThat(referral.getFirstName()).isEqualTo("John");
        assertThat(referral.getMiddleName()).isEqualTo("Middle");
        assertThat(referral.getLastName()).isEqualTo("Smith");
        assertThat(referral.getAgencyLocationId()).isEqualTo("LEI");
        assertThat(referral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(referral.getDeletionDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 2, 3, 4, 5));

        assertThat(referral.getOffenderAliases()).hasSize(1);
        assertMatchesExpectedContents(referral.getOffenderAliases().get(0));
    }

    private void assertMatchesExpectedContents(final ReferredOffenderNoBookingAlias offenderBooking) {
        assertThat(offenderBooking.getOffenderId()).isEqualTo(-1001L);
    }


}
