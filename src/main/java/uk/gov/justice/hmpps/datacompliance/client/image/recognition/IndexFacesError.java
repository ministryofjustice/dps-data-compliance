package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IndexFacesError {
    FACE_NOT_FOUND("FACE_NOT_FOUND"),
    FACE_POOR_QUALITY("FACE_POOR_QUALITY"),
    MULTIPLE_FACES_FOUND("MULTIPLE_FACES_FOUND");

    private final String reason;
}
