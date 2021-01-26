package uk.gov.justice.hmpps.datacompliance.utils.web.request;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@Builder
@Getter
@JsonInclude(NON_NULL)
public class ManualRetentionRequest {


    @Singular
    private List<ManualRetentionReason> retentionReasons;
}
