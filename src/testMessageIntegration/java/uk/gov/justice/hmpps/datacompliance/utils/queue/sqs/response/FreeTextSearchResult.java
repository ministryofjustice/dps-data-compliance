package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
public class FreeTextSearchResult {

    private String offenderIdDisplay;
    private Long retentionCheckId;
    @Singular
    private List<String> matchingTables;
}
