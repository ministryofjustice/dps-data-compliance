package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class OffenderDeletionReferralRequest {
    private final Long batchId;
    private final LocalDate dueForDeletionWindowStart;
    private final LocalDate dueForDeletionWindowEnd;
    private final Integer limit;
}
