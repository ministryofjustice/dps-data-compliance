package uk.gov.justice.hmpps.datacompliance.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class OffenderToCheck {
    private final OffenderNumber offenderNumber;
    @Singular private final List<String> offenceCodes;
    @Singular private final List<String> alertCodes;
    @Singular private final Set<String> bookingNos;
    @Singular private final Set<String> pncs;
    @Singular private final Set<String> cros;
    private final String firstName;
    private final String middleName;
    private final String lastName;

    public String getFirstNames(){
        if (hasText(middleName)) return firstName + " " + middleName;
        return getFirstName();
    }
}
