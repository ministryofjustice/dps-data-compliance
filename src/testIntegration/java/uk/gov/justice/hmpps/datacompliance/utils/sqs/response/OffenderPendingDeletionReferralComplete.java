package uk.gov.justice.hmpps.datacompliance.utils.sqs.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OffenderPendingDeletionReferralComplete {

    private Long batchId;

    private Long numberReferred;

    private Long totalInWindow;
}
