package uk.gov.justice.hmpps.datacompliance.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(exclude = { "firstName", "middleName", "lastName", "birthDate" })
public class OffenderPendingDeletionEvent {

    @JsonProperty("offenderIdDisplay")
    private String offenderIdDisplay;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("middleName")
    private String middleName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("birthDate")
    private LocalDate birthDate;

    @Singular
    @JsonProperty("offenders")
    private List<OffenderWithBookings> offenders;

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffenderWithBookings {

        @JsonProperty("offenderId")
        private Long offenderId;

        @Singular
        @JsonProperty("bookings")
        private List<Booking> bookings;
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Booking {

        @JsonProperty("offenderBookId")
        private Long offenderBookId;
    }
}
