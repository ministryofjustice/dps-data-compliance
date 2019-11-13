package uk.gov.justice.hmpps.datacompliance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class OffenderDeletionEvent {
    private String offenderIdDisplay;
}

