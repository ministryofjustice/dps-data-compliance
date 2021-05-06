package uk.gov.justice.hmpps.datacompliance.client.govnotify;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class EmailParameters implements Mappable{

    private String omu;
    private String offenderName;
    private String nomisNumber;
    private String dpsRetentionUrl;
    private String reviewPeriodDuration;

}
