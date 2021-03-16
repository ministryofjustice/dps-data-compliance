package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OffenderDeletionReferralRepository extends CrudRepository<OffenderDeletionReferral, Long> {


    List<OffenderDeletionReferral> findByOffenderNo(final String offenderNo);
    List<OffenderDeletionReferral> findByAgencyLocationId(final String agencyLocationId);
    @Query(
        value = "SELECT * FROM offender_deletion_referral odr " +
            "LEFT JOIN referral_resolution rr " +
            "ON rr.referral_id = odr.referral_id " +
            "LEFT JOIN referred_offender_alias roa " +
            "ON roa.referral_id = odr.referral_id " +
            "WHERE rr.resolution_status = :resolutionStatus " +
            "AND rr.resolution_date_time < :deleteAfterDateTime " +
            "AND rr.provisional_deletion_previously_granted = :provisionalDeletionPreviouslyGranted " +
            "ORDER BY rr.resolution_date_time ASC " +
            "LIMIT :limit",  nativeQuery = true)
    List<OffenderDeletionReferral> findByReferralResolutionStatus(final String resolutionStatus, final Boolean provisionalDeletionPreviouslyGranted, final LocalDateTime deleteAfterDateTime, long limit);

}
