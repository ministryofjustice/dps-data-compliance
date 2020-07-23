package uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffendersWithImagesResponse {

    @Singular
    @JsonProperty("content")
    private List<OffenderNumber> offenderNumbers;

    @JsonProperty("totalElements")
    private Long totalElements;
}
