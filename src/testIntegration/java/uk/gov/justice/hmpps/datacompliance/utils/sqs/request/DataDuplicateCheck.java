package uk.gov.justice.hmpps.datacompliance.utils.sqs.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Builder
@JsonInclude(NON_NULL)
public class DataDuplicateCheck {

    private String offenderIdDisplay;

    private Long retentionCheckId;
}

