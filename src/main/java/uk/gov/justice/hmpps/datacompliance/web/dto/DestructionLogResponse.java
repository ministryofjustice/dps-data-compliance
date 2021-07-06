package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@ApiModel(description = "A log of offender deletions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class DestructionLogResponse {

    @ApiModelProperty(required = true, value = "This is known as the 'offender number")
    @NotNull
    @JsonProperty("nomsId")
    private String nomisId;


    @ApiModelProperty(required = true, value = "The first and middle names of the offender")
    @NotNull
    @JsonProperty("firstName")
    private String firstName;

    @ApiModelProperty(required = true, value = "The offenders middle names")
    @NotNull
    @JsonProperty("middleName")
    private String middleName;


    @ApiModelProperty(required = true, value = "The offenders last name")
    @NotNull
    @JsonProperty("lastName")
    private String lastName;


    @ApiModelProperty(required = true, value = "The timestamp of the offenders date of birth (Format: ISO DATE- yyyy-MM-dd)")
    @NotNull
    @JsonProperty("dateOfBirth")
    @DateTimeFormat(iso = DATE)
    private LocalDate dateOfBirth;


    @ApiModelProperty(required = true, value = "The of type of record that was destroyed.")
    @NotNull
    @JsonProperty("typeOfRecordDestroyed")
    private String typeOfRecordDestroyed;


    @ApiModelProperty(required = true, value = "The timestamp of when this offender record was destroyed.")
    @NotNull
    @JsonProperty("destructionDate")
    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime destructionDate;

    @ApiModelProperty(required = true, value = "The method of destruction utilised.")
    @NotNull
    @JsonProperty("methodOfDestruction")
    private String methodOfDestruction;

    @ApiModelProperty(required = true, value = "The authoriser of the record destruction.")
    @NotNull
    @JsonProperty("authorisationOfDestruction")
    private String authorisationOfDestruction;
}
