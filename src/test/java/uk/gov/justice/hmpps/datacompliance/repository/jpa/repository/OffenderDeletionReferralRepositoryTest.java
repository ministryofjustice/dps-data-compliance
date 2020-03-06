package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ReferredOffenderBooking;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class OffenderDeletionReferralRepositoryTest {

    @Autowired
    private OffenderDeletionReferralRepository repository;

    @Test
    @Sql("offender_deletion_referral.sql")
    @Sql("referred_offender_booking.sql")
    void getOffenderDeletionReferral() {
        assertMatchesExpectedContents(repository.findById(1L).orElseThrow());
    }

    @Test
    void saveOffenderDeletionReferral() {

        final var referral = OffenderDeletionReferral.builder()
                .receivedDateTime(LocalDateTime.of(2020, 1, 1, 1, 2, 3))
                .offenderNo("A1234AA")
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .build();

        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder()
                .offenderId(-1001L)
                .offenderBookId(-1L)
                .build());

        repository.save(referral);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertMatchesExpectedContents(repository.findById(referral.getReferralId()).orElseThrow());
    }

    private void assertMatchesExpectedContents(final OffenderDeletionReferral referral) {
        assertThat(referral.getOffenderNo()).isEqualTo("A1234AA");
        assertThat(referral.getFirstName()).isEqualTo("John");
        assertThat(referral.getMiddleName()).isEqualTo("Middle");
        assertThat(referral.getLastName()).isEqualTo("Smith");
        assertThat(referral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(referral.getReceivedDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 1, 1, 2, 3));

        assertThat(referral.getReferredOffenderBookings()).hasSize(1);
        final var offenderBooking = referral.getReferredOffenderBookings().get(0);

        assertThat(offenderBooking.getOffenderId()).isEqualTo(-1001L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(-1L);
    }
}