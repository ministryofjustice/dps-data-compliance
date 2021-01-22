package uk.gov.justice.hmpps.datacompliance.utils.sqs.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@Getter
@JsonInclude(NON_NULL)
public class OffenderDeletionGranted {

    private String offenderIdDisplay;

    private Long referralId;

    @Singular
    private Set<Long> offenderIds;

    @Singular
    private Set<Long> offenderBookIds;
}
