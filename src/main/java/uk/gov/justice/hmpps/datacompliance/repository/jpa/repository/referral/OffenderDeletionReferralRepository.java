package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;

import java.util.List;

@Repository
public interface OffenderDeletionReferralRepository extends CrudRepository<OffenderDeletionReferral, Long> {


    List<OffenderDeletionReferral> findByOffenderNo(final String offenderNo);
    List<OffenderDeletionReferral> findByAgencyLocationId(final String agencyLocationId);
}
