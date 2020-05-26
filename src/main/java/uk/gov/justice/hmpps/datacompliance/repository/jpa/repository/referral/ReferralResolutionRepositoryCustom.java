package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import javax.persistence.LockModeType;

public interface ReferralResolutionRepositoryCustom {
    void lock(Object entity, LockModeType lockMode);
}
