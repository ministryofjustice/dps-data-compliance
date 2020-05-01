package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;

import java.util.List;

import static java.util.Collections.emptyList;

@Service
@AllArgsConstructor
public class DataDuplicationDetectionService {
    public List<DataDuplicate> findDuplicatesFor(final OffenderNumber offenderNumber) {
        // TODO GDPR-111 Implement duplicate data detection check
        return emptyList();
    }
}
