package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class FaceId {
    private final String faceId;
}
