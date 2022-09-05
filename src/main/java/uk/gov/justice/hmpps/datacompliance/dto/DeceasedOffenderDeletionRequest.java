package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class DeceasedOffenderDeletionRequest {

    private final Long batchId;
    private final Set<String> excludedOffenders;
    private final Integer limit;

}
