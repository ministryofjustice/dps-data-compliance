package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.Set;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class DeceasedOffenderDeletionRequest {

    private final Long batchId;
    @Builder.Default
    private final Set<String> excludedOffenders = Collections.emptySet();
    private final Integer limit;

}
