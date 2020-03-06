package uk.gov.justice.hmpps.datacompliance.events.dto;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class OffenderPendingDeletionEvent {
    private String offenderIdDisplay;
}

