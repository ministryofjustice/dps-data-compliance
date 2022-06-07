package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.offendernobooking;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionReferral;

import java.util.List;

@Repository
public interface OffenderNoBookingDeletionReferralRepository extends CrudRepository<OffenderNoBookingDeletionReferral, Long> {


    List<OffenderNoBookingDeletionReferral> findByOffenderNo(final String offenderNo);

    List<OffenderNoBookingDeletionReferral> findByAgencyLocationId(final String agencyLocationId);

}
