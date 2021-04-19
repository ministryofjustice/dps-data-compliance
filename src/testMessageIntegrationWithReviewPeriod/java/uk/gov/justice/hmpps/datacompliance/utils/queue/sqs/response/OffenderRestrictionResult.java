package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenderRestrictionResult {

    private String offenderIdDisplay;

    private Long retentionCheckId;

    private boolean restricted;
}
