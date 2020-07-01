package uk.gov.justice.hmpps.datacompliance.events.listeners.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(exclude = { "firstName", "middleName", "lastName", "birthDate" })
public class OffenderPendingDeletion {

    @JsonProperty("offenderIdDisplay")
    private String offenderIdDisplay;

    @JsonProperty("batchId")
    private Long batchId;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("middleName")
    private String middleName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("birthDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate birthDate;

    @Singular
    @JsonProperty("offenders")
    private List<OffenderWithBookings> offenders;

    @JsonIgnore
    public List<String> getOffenceCodes() {
        return offenders.stream()
                .map(OffenderWithBookings::getOffenderBookings)
                .flatMap(Collection::stream)
                .map(OffenderBooking::getOffenceCodes)
                .flatMap(Collection::stream)
                .collect(toList());
    }

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
        private List<OffenderBooking> offenderBookings;
    }

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffenderBooking {

        @JsonProperty("offenderBookId")
        private Long offenderBookId;

        @Singular
        @JsonProperty("offenceCodes")
        private Set<String> offenceCodes;
    }
}
