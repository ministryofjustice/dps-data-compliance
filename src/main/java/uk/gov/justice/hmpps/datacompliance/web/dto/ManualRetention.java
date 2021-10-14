package uk.gov.justice.hmpps.datacompliance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Schema(description = "Offender data retention details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ManualRetention {

    @Schema(required = true, description = "The offender's unique offender number (aka NOMS number).")
    @NotNull
    @JsonProperty("offenderNo")
    private String offenderNo;

    @Schema(required = true, description = "The user id of the member of staff who last modified this retention record.")
    @NotNull
    @JsonProperty("userId")
    private String userId;

    @Schema(required = true, description = "The timestamp of when this retention record was last modified.")
    @NotNull
    @JsonProperty("modifiedDateTime")
    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime modifiedDateTime;

    @Schema(required = true, description = "The list of reason codes for why the offender record should be retained")
    @NotNull
    @Singular
    @JsonProperty("retentionReasons")
    private List<ManualRetentionReason> retentionReasons;

}
