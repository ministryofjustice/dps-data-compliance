package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Schema(description = "Ual Offender data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class UalOffenderResponse {

    @Schema(description = "This is known as the 'offender number")
    @JsonProperty("nomsId")
    private String nomsId;

    @Schema(description = "The offenders prisonNumber (book number)")
    @JsonProperty("prisonNumber")
    private String prisonNumber;

    @Schema(description = "The PNC number")
    @JsonProperty("pnc")
    private String pnc;

    @Schema(description = "The CRO number")
    @JsonProperty("cro")
    private String cro;

    @Schema(description = "The first and middle names of the offender")
    @JsonProperty("firstNames")
    private String firstNames;

    @Schema(description = "The offenders last name")
    @JsonProperty("familyName")
    private String familyName;

    @Schema(description = "The description of an offence relating to the offender")
    @JsonProperty("indexOffenceDescription")
    private String indexOffenceDescription;
}
