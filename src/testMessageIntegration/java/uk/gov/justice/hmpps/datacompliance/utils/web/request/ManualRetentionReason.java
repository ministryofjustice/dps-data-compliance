package uk.gov.justice.hmpps.datacompliance.utils.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Builder

@JsonInclude(NON_NULL)
public class ManualRetentionReason {

    private ManualRetentionReasonCode reasonCode;

    private String reasonDetails;
}
