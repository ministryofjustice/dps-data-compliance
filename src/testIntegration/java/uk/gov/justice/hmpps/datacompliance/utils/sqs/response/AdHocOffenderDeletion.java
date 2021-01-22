package uk.gov.justice.hmpps.datacompliance.utils.sqs.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdHocOffenderDeletion {

    private String offenderIdDisplay;
    private String reason;
}
