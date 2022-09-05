package uk.gov.justice.hmpps.datacompliance.events.publishers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Set<String> excludedOffenders;

    @JsonProperty("limit")
    private Integer limit;
}

