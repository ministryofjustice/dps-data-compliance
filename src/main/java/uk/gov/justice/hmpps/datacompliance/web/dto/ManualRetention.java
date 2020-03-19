package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@ApiModel(description = "Offender data retention details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ManualRetention {

    @ApiModelProperty(required = true, value = "The offender's unique offender number (aka NOMS number).")
    @NotNull
    @JsonProperty("offenderNo")
    private String offenderNo;

    @ApiModelProperty(required = true, value = "The unique staff id of the member of staff who last modified this retention record.")
    @NotNull
    @JsonProperty("staffId")
    private Long staffId;

    @ApiModelProperty(required = true, value = "The timestamp of when this retention record was last modified.")
    @NotNull
    @JsonProperty("modifiedDateTime")
    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime modifiedDateTime;

    @ApiModelProperty(required = true, value = "The list of reason codes for why the offender record should be retained")
    @NotNull
    @Singular
    @JsonProperty("retentionReasons")
    private List<ManualRetentionReason> retentionReasons;

}
