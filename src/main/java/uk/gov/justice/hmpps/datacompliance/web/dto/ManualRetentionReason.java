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

@Schema(description = "Retention reason")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ManualRetentionReason {

    @Schema(required = true, description = "The code identifying a particular retention reason")
    @NotNull
    @JsonProperty("reasonCode")
    private ManualRetentionReasonCode reasonCode;

    @Schema(description = "Additional details about the reason for retention")
    @JsonProperty("reasonDetails")
    private String reasonDetails;
}
