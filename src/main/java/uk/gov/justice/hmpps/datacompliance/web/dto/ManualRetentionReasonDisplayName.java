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

@Schema(description = "Retention reason code and display name")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ManualRetentionReasonDisplayName {

    @Schema(required = true, description = "The code identifying a particular retention reason")
    @NotNull
    @JsonProperty("reasonCode")
    private ManualRetentionReasonCode reasonCode;

    @Schema(description = "The UI display name")
    @NotNull
    @JsonProperty("displayName")
    private String displayName;

    @Schema(description = "Flag identifying if end user is allowed to enter details about the retention reason")
    @NotNull
    @JsonProperty("allowReasonDetails")
    private Boolean allowReasonDetails;

    @Schema(description = "Order in which the reasons are displayed to the end user")
    @NotNull
    @JsonProperty("displayOrder")
    private Integer displayOrder;
}
