package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ApiModel(description = "A log of offender deletions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "NOMIS_ID", "FIRST_NAME", "MIDDLE_NAME", "LAST_NAME", "DATE_OF_BIRTH", "LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT",
    "TYPE_OF_RECORD_DESTROYED", "DESTRUCTION_DATE", "METHOD_OF_DESTRUCTION", "AUTHORISATION_OF_DESTRUCTION"})
public class DestructionLogResponse {

    @ApiModelProperty(required = true, value = "This is known as the 'offender number")
    @NotNull
    @JsonProperty("NOMIS_ID")
    private String nomisId;


    @ApiModelProperty(required = true, value = "The first and middle names of the offender")
    @NotNull
    @JsonProperty("FIRST_NAME")
    private String firstName;

    @ApiModelProperty(required = true, value = "The offenders middle names")
    @NotNull
    @JsonProperty("MIDDLE_NAME")
    private String middleName;


    @ApiModelProperty(required = true, value = "The offenders last name")
    @NotNull
    @JsonProperty("LAST_NAME")
    private String lastName;


    @ApiModelProperty(required = true, value = "The timestamp of the offenders date of birth (Format: ISO DATE- yyyy-MM-dd)")
    @NotNull
    @JsonProperty("DATE_OF_BIRTH")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate dateOfBirth;

    @ApiModelProperty(required = true, value = "The last known offender management unit at which the offender resided.")
    @NotNull
    @JsonProperty("LAST_KNOWN_OFFENDER_MANAGEMENT_UNIT")
    private String lastKnownOmu;

    @ApiModelProperty(required = true, value = "The of type of record that was destroyed.")
    @NotNull
    @JsonProperty("TYPE_OF_RECORD_DESTROYED")
    private String typeOfRecordDestroyed;


    @ApiModelProperty(required = true, value = "The timestamp of when this offender record was destroyed.")
    @NotNull
    @JsonProperty("DESTRUCTION_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime destructionDate;

    @ApiModelProperty(required = true, value = "The method of destruction utilised.")
    @NotNull
    @JsonProperty("METHOD_OF_DESTRUCTION")
    private String methodOfDestruction;

    @ApiModelProperty(required = true, value = "The authoriser of the record destruction.")
    @NotNull
    @JsonProperty("AUTHORISATION_OF_DESTRUCTION")
    private String authorisationOfDestruction;
}
