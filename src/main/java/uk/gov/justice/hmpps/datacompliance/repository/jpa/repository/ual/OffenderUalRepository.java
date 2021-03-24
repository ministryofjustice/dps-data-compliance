package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;

import java.util.List;
import java.util.Optional;


@Repository
public interface OffenderUalRepository extends CrudRepository<OffenderUalEntity, Long> {

    Optional<OffenderUalEntity> findOneByOffenderNo(final String offenderNo);

    Optional<OffenderUalEntity> findOneByOffenderBookingNoAndOffenderCroPncAndFirstNamesAndLastName(final String offenderBookingNo, final String offenderCroPnc, final String firstNames, final String lastName);

    void deleteByOffenderUalIdNotIn(final List<Long> ids);
}
