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

@ApiModel(description = "Retention reason code and display name")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ManualRetentionReasonDisplayName {

    @ApiModelProperty(required = true, value = "The code identifying a particular retention reason")
    @NotNull
    @JsonProperty("reasonCode")
    private ManualRetentionReasonCode reasonCode;

    @ApiModelProperty(value = "The UI display name")
    @JsonProperty("displayName")
    private String displayName;
}
