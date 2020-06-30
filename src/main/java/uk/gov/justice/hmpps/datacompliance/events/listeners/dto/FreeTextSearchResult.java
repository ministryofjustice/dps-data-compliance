package uk.gov.justice.hmpps.datacompliance.events.listeners.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeTextSearchResult {

    @JsonProperty("offenderIdDisplay")
    private String offenderIdDisplay;

    @JsonProperty("retentionCheckId")
    private Long retentionCheckId;

    @Singular
    @JsonProperty("matchingTables")
    private List<String> matchingTables;
}
