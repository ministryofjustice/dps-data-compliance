package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;

import java.util.Optional;


@Repository
public interface OffenderUalRepository extends CrudRepository<OffenderUalEntity, Long> {

    Optional<OffenderUalEntity> findOneByOffenderNoIgnoreCase(final String offenderNo);

    Optional<OffenderUalEntity> findOneByOffenderBookingNoIgnoreCase(final String offenderBookingNo);

    Optional<OffenderUalEntity> findOneByOffenderPncIgnoreCase(final String pnc);

    Optional<OffenderUalEntity> findOneByOffenderCroIgnoreCase(final String cro);
}
