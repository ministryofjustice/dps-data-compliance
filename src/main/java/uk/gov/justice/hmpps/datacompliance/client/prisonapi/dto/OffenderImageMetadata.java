package uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenderImageMetadata {

    private static final String FACE_IMAGE_VIEW_TYPE = "FACE";

    @JsonProperty("imageId")
    private Long imageId;

    @JsonProperty("imageView")
    private String imageView;

    public boolean isOffenderFaceImage() {
        return imageId != null && FACE_IMAGE_VIEW_TYPE.equalsIgnoreCase(imageView);
    }
}
