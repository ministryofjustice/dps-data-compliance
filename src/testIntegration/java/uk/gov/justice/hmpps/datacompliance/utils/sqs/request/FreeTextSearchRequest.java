package uk.gov.justice.hmpps.datacompliance.utils.sqs.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FreeTextSearchRequest {

    private String offenderIdDisplay;

    private Long retentionCheckId;

    private List<String> regex;
}

