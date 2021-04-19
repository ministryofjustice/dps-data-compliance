package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;


@Getter
@Builder
public class ProvisionalDeletionReferralResult {

    private Long referralId;

    private String offenderIdDisplay;

    private boolean subsequentChangesIdentified;

    private String agencyLocationId;

    @Singular
    private Set<String> offenceCodes;

    @Singular
    private Set<String> alertCodes;

}

