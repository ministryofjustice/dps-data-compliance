package uk.gov.justice.hmpps.datacompliance.services.events.dto;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class OffenderPendingDeletionEvent {
    private String offenderIdDisplay;
}

