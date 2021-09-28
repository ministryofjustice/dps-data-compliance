package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionReferral;

import java.util.List;

@Repository
public interface DeceasedOffenderDeletionReferralRepository extends CrudRepository<DeceasedOffenderDeletionReferral, Long> {


    List<DeceasedOffenderDeletionReferral> findByOffenderNo(final String offenderNo);

    List<DeceasedOffenderDeletionReferral> findByAgencyLocationId(final String agencyLocationId);

}
