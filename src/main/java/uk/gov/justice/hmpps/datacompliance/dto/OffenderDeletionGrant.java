package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Set;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class OffenderDeletionGrant {
    private final OffenderNumber offenderNumber;
    private final Long referralId;
    @Singular private final Set<Long> offenderIds;
    @Singular private final Set<Long> offenderBookIds;
}
