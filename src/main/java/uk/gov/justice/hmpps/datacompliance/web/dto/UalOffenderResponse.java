package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Schema(description = "Ual Offender data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class UalOffenderResponse {

    @Schema(required = true, description = "This is known as the 'offender number")
    @NotNull
    @JsonProperty("nomsId")
    private String nomsId;

    @Schema(required = true, description = "The offenders prisonNumber (book number)")
    @NotNull
    @JsonProperty("prisonNumber")
    private String prisonNumber;

    @Schema(required = true, description = "This may contain the CRO number or the PNC number or both")
    @NotNull
    @JsonProperty("croPnc")
    private String croPnc;

    @Schema(required = true, description = "The first and middle names of the offender")
    @NotNull
    @JsonProperty("firstNames")
    private String firstNames;

    @Schema(required = true, description = "The offenders last name")
    @NotNull
    @JsonProperty("familyName")
    private String familyName;

    @Schema(required = true, description = "The description of an offence relating to the offender")
    @NotNull
    @JsonProperty("indexOffenceDescription")
    private String indexOffenceDescription;
}
