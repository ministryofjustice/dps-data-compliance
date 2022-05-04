package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UalOffender {

    private String nomsId;
    private String prisonNumber;
    private String croPnc;
    private String firstNames;
    private String familyName;
    private String indexOffenceDescription;

}
