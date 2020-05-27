package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import lombok.AllArgsConstructor;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@AllArgsConstructor
public class ReferralResolutionRepositoryImpl implements ReferralResolutionRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void lock(final Object entity, final LockModeType lockMode) {
        entityManager.lock(entity, lockMode);
    }
}
