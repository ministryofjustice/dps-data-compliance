package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.offendernobooking;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionBatch.BatchType;

import java.util.List;
import java.util.Optional;

@Repository
public interface OffenderNoBookingDeletionBatchRepository extends CrudRepository<OffenderNoBookingDeletionBatch, Long> {

    Optional<OffenderNoBookingDeletionBatch> findFirstByBatchTypeOrderByRequestDateTimeDesc(BatchType batchType);

    List<OffenderNoBookingDeletionBatch> findByReferralCompletionDateTimeIsNull();
}
