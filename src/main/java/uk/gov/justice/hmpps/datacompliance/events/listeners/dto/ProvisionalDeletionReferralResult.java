package uk.gov.justice.hmpps.datacompliance.events.listeners.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.springframework.util.StringUtils.hasText;


@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProvisionalDeletionReferralResult {

    @JsonProperty("referralId")
    private Long referralId;

    @JsonProperty("offenderIdDisplay")
    private String offenderIdDisplay;

    @JsonProperty("subsequentChangesIdentified")
    private boolean subsequentChangesIdentified;

    @JsonProperty("agencyLocationId")
    private String agencyLocationId;

    @Singular
    @JsonProperty("offenceCodes")
    private Set<String> offenceCodes;

    @Singular
    @JsonProperty("alertCodes")
    private Set<String> alertCodes;


    @JsonIgnore
    public boolean haveSubsequentChangesOccurred(final String agencyLocationId) {
        return this.subsequentChangesIdentified || this.offenderHasMoved(agencyLocationId);
    }

    @JsonIgnore
    private boolean offenderHasMoved(String previousAgencyLoc) {
        return hasText(previousAgencyLoc) && !previousAgencyLoc.equalsIgnoreCase(this.agencyLocationId);
    }
}

