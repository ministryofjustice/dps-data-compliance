package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class OffenderImage {
    private final OffenderNumber offenderNumber;
    private final long imageId;
    private final byte[] imageData;

    public String getOffenderNumberString() {
        return offenderNumber.getOffenderNumber();
    }
}
