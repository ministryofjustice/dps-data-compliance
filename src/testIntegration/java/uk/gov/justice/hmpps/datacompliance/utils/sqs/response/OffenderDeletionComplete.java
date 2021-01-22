package uk.gov.justice.hmpps.datacompliance.utils.sqs.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OffenderDeletionComplete {

    private String offenderIdDisplay;
    private Long referralId;
}
