package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

@Service
@AllArgsConstructor
public class DataDuplicationDetectionService {
    public void searchForDuplicates(final OffenderNumber offenderNumber, final Long checkId) {
        // TODO GDPR-111 Implement asynchronous duplicate data detection check
    }
}
