package uk.gov.justice.hmpps.datacompliance.events.publishers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class DeceasedDeletionRequest {

    @JsonProperty("batchId")
    private Long batchId;

    @JsonProperty("excludedOffenders")
    @Builder.Default
    private Set<String> excludedOffenders = Collections.emptySet();

    @JsonProperty("limit")
    private Integer limit;
}

