package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ApiModel(description = "Ual Offender data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class UalOffenderResponse {

    @ApiModelProperty(required = true, value = "This is known as the 'offender number")
    @NotNull
    @JsonProperty("nomsId")
    private String nomsId;

    @ApiModelProperty(required = true, value = "The offenders prisonNumber (book number)")
    @NotNull
    @JsonProperty("prisonNumber")
    private String prisonNumber;

    @ApiModelProperty(required = true, value = "This may contain the CRO number or the PNC number or both")
    @NotNull
    @JsonProperty("croPnc")
    private String croPnc;

    @ApiModelProperty(required = true, value = "The first and middle names of the offender")
    @NotNull
    @JsonProperty("firstNames")
    private String firstNames;

    @ApiModelProperty(required = true, value = "The offenders last name")
    @NotNull
    @JsonProperty("familyName")
    private String familyName;

    @ApiModelProperty(required = true, value = "The description of an offence relating to the offender")
    @NotNull
    @JsonProperty("indexOffenceDescription")
    private String indexOffenceDescription;
}
