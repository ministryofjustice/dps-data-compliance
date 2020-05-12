package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

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
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.ManualRetentionRepository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate.IMAGE_DUPLICATE;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual.MANUAL_RETENTION;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder.PATHFINDER_REFERRAL;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class OffenderDeletionReferralRepositoryTest {

    @Autowired
    private ImageDuplicateRepository imageDuplicateRepository;

    @Autowired
    private ManualRetentionRepository manualRetentionRepository;

    @Autowired
    private OffenderDeletionBatchRepository batchRepository;

    @Autowired
    private OffenderDeletionReferralRepository repository;

    @Test
    @Sql("image_upload_batch.sql")
    @Sql("offender_image_upload.sql")
    @Sql("image_duplicate.sql")
    @Sql("offender_deletion_batch.sql")
    @Sql("offender_deletion_referral.sql")
    @Sql("referred_offender_booking.sql")
    @Sql("referral_resolution.sql")
    @Sql("manual_retention.sql")
    @Sql("retention_check.sql")
    @Sql("retention_reason_manual.sql")
    @Sql("retention_reason_image_duplicate.sql")
    void getOffenderDeletionReferral() {
        assertMatchesExpectedContents(repository.findById(1L).orElseThrow());
    }

    @Test
    @Sql("offender_deletion_batch.sql")
    @Sql("offender_deletion_referral.sql")
    void getOffenderDeletionReferralWithoutBookingsOrResolution() {
        final var referral = repository.findById(1L).orElseThrow();

        assertThat(referral.getOffenderBookings()).isEmpty();
        assertThat(referral.getReferralResolution()).isEmpty();
    }

    @Test
    @Sql("image_upload_batch.sql")
    @Sql("offender_image_upload.sql")
    @Sql("image_duplicate.sql")
    @Sql("offender_deletion_batch.sql")
    @Sql("manual_retention.sql")
    void saveOffenderDeletionReferral() {

        final var referral = offenderDeletionReferral();

        repository.save(referral);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertMatchesExpectedContents(repository.findById(referral.getReferralId()).orElseThrow());
    }

    private OffenderDeletionReferral offenderDeletionReferral() {
        final var referral = OffenderDeletionReferral.builder()
                .receivedDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))
                .offenderDeletionBatch(batchRepository.findById(1L).orElseThrow())
                .offenderNo("A1234AA")
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .build();

        referral.setReferralResolution(referralResolution());
        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder()
                .offenderId(-1001L)
                .offenderBookId(-1L)
                .build());

        return referral;
    }

    private ReferralResolution referralResolution() {
        return ReferralResolution.builder()
                .resolutionStatus(PENDING)
                .resolutionDateTime(LocalDateTime.of(2021, 2, 3, 4, 5, 6))
                .build()

                .addRetentionCheck(new RetentionCheckManual(Status.PENDING)
                        .setManualRetention(manualRetentionRepository.findById(1L).orElseThrow()))
                .addRetentionCheck(new RetentionCheckPathfinder(RETENTION_REQUIRED))
                .addRetentionCheck(new RetentionCheckImageDuplicate(RETENTION_NOT_REQUIRED)
                        .addImageDuplicate(imageDuplicateRepository.findById(1L).orElseThrow()));
    }

    private void assertMatchesExpectedContents(final OffenderDeletionReferral referral) {
        assertThat(referral.getOffenderNo()).isEqualTo("A1234AA");
        assertThat(referral.getFirstName()).isEqualTo("John");
        assertThat(referral.getMiddleName()).isEqualTo("Middle");
        assertThat(referral.getLastName()).isEqualTo("Smith");
        assertThat(referral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(referral.getReceivedDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 2, 3, 4, 5));

        assertThat(referral.getOffenderBookings()).hasSize(1);
        assertMatchesExpectedContents(referral.getOffenderBookings().get(0));
        assertMatchesExpectedContents(referral.getReferralResolution().orElseThrow());
    }

    private void assertMatchesExpectedContents(final ReferredOffenderBooking offenderBooking) {
        assertThat(offenderBooking.getOffenderId()).isEqualTo(-1001L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(-1L);
    }

    private void assertMatchesExpectedContents(final ReferralResolution resolution) {
        assertThat(resolution.getResolutionDateTime()).isEqualTo(LocalDateTime.of(2021, 2, 3, 4, 5, 6));
        assertThat(resolution.getResolutionStatus()).isEqualTo(PENDING);
        assertThat(resolution.isType(PENDING)).isTrue();

        assertMatchesExpectedContents(resolution.getRetentionChecks());
    }

    private void assertMatchesExpectedContents(final List<RetentionCheck> retentionChecks) {

        assertThat(retentionChecks).hasSize(3);

        final var manualRetentionCheck = getRetentionCheck(retentionChecks, MANUAL_RETENTION, RetentionCheckManual.class);
        assertThat(manualRetentionCheck.getCheckStatus()).isEqualTo(Status.PENDING);
        assertThat(manualRetentionCheck.getManualRetention().getManualRetentionId()).isEqualTo(1L);

        final var pathfinderReferralCheck = getRetentionCheck(retentionChecks, PATHFINDER_REFERRAL, RetentionCheckPathfinder.class);
        assertThat(pathfinderReferralCheck.getCheckStatus()).isEqualTo(RETENTION_REQUIRED);

        final var imageDuplicateCheck = getRetentionCheck(retentionChecks, IMAGE_DUPLICATE, RetentionCheckImageDuplicate.class);
        assertThat(imageDuplicateCheck.getCheckStatus()).isEqualTo(RETENTION_NOT_REQUIRED);
        assertThat(imageDuplicateCheck.getImageDuplicates()).extracting(ImageDuplicate::getImageDuplicateId).containsExactly(1L);
    }

    private <T extends RetentionCheck> T getRetentionCheck(final List<RetentionCheck> retentionChecks,
                                                           final String checkType,
                                                           final Class<T> retentionCheckClass) {
        return retentionChecks.stream()
                .filter(check -> checkType.equals(check.getCheckType()))
                .map(retentionCheckClass::cast)
                .findFirst()
                .orElseThrow();
    }
}
