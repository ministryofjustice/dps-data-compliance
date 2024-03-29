package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.ManualRetentionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

class OffenderDeletionReferralRepositoryTest extends JpaRepositoryTest {

    @Autowired
    private ImageDuplicateRepository imageDuplicateRepository;

    @Autowired
    private ManualRetentionRepository manualRetentionRepository;

    @Autowired
    private OffenderDeletionBatchRepository batchRepository;

    @Autowired
    private OffenderDeletionReferralRepository repository;

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
    void getOffenderDeletionReferral() {
        assertMatchesExpectedContents(repository.findById(1L).orElseThrow());
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
    void getOffenderDeletionReferralSByAgencyLocationId() {
        assertMatchesExpectedContents(repository.findByAgencyLocationId("LEI").get(0));
    }

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    void getOffenderDeletionReferralWithoutBookingsOrResolution() {
        final var referral = repository.findById(1L).orElseThrow();

        assertThat(referral.getOffenderAliases()).isEmpty();
        assertThat(referral.getReferralResolution()).isEmpty();
    }

    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    @Sql("classpath:seed.data/image_duplicate.sql")
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    void saveOffenderDeletionReferral() {

        final var referral = offenderDeletionReferral();

        repository.save(referral);

        assertMatchesExpectedContents(repository.findById(referral.getReferralId()).orElseThrow());
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
    void getByReferralResolutionStatus() {
        final var beforeReferralResolution = LocalDateTime.of(2021, 02, 03, 04, 05, 05);
        assertThat(repository.findByReferralResolutionStatus("PENDING", false, beforeReferralResolution, 1L)).isEmpty();

        final var afterReferralResolution = LocalDateTime.of(2021, 02, 03, 05, 05, 04);
        assertMatchesExpectedContents(repository.findByReferralResolutionStatus("PENDING", false, afterReferralResolution, 1L).get(0));
    }

    private OffenderDeletionReferral offenderDeletionReferral() {
        final var referral = OffenderDeletionReferral.builder()
            .receivedDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))
            .offenderDeletionBatch(batchRepository.findById(1L).orElseThrow())
            .offenderNo("A1234AA")
            .firstName("John")
            .middleName("Middle")
            .lastName("Smith")
            .agencyLocationId("LEI")
            .birthDate(LocalDate.of(1969, 1, 1))
            .build();

        referral.setReferralResolution(referralResolution());
        referral.addReferredOffenderAlias(ReferredOffenderAlias.builder()
            .offenderId(-1001L)
            .offenderBookId(-1L)
            .build());

        return referral;
    }

    private ReferralResolution referralResolution() {
        return ReferralResolution.builder()
            .resolutionStatus(PENDING)
            .resolutionDateTime(LocalDateTime.of(2021, 2, 3, 4, 5, 6))
            .provisionalDeletionPreviouslyGranted(false)
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
        assertThat(referral.getAgencyLocationId()).isEqualTo("LEI");
        assertThat(referral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(referral.getReceivedDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 2, 3, 4, 5));

        assertThat(referral.getOffenderAliases()).hasSize(1);
        assertMatchesExpectedContents(referral.getOffenderAliases().get(0));
        assertMatchesExpectedContents(referral.getReferralResolution().orElseThrow());
    }

    private void assertMatchesExpectedContents(final ReferredOffenderAlias offenderBooking) {
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

        final var manualRetentionCheck = getRetentionCheck(retentionChecks, RetentionCheckManual.class);
        assertThat(manualRetentionCheck.getCheckStatus()).isEqualTo(Status.PENDING);
        assertThat(manualRetentionCheck.getManualRetention().getManualRetentionId()).isEqualTo(1L);

        final var pathfinderReferralCheck = getRetentionCheck(retentionChecks, RetentionCheckPathfinder.class);
        assertThat(pathfinderReferralCheck.getCheckStatus()).isEqualTo(RETENTION_REQUIRED);

        final var imageDuplicateCheck = getRetentionCheck(retentionChecks, RetentionCheckImageDuplicate.class);
        assertThat(imageDuplicateCheck.getCheckStatus()).isEqualTo(RETENTION_NOT_REQUIRED);
        assertThat(imageDuplicateCheck.getImageDuplicates()).extracting(ImageDuplicate::getImageDuplicateId).containsExactly(1L);
    }

    private <T extends RetentionCheck> T getRetentionCheck(final List<RetentionCheck> retentionChecks,
                                                           final Class<T> retentionCheckClass) {
        return retentionChecks.stream()
            .filter(retentionCheckClass::isInstance)
            .map(retentionCheckClass::cast)
            .findFirst()
            .orElseThrow();
    }
}
