package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;

@Repository
public interface ReferralResolutionRepository extends CrudRepository<ReferralResolution, Long> {
}
