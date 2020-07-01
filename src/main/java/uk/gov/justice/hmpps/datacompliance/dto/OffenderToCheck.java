package uk.gov.justice.hmpps.datacompliance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenderToCheck {

    private final OffenderNumber offenderNumber;

    @Singular
    private final List<String> offenceCodes;
}
