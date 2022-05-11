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
@ToString(exclude = {"firstName", "middleName", "lastName", "birthDate"})
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

    @Singular
    @JsonProperty("pncs")
    private Set<String> pncs;

    @Singular
    @JsonProperty("cros")
    private Set<String> cros;

    @JsonProperty("birthDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate birthDate;

    @JsonProperty("agencyLocationId")
    private String agencyLocationId;

    @Singular
    @JsonProperty("offenderAliases")
    private List<OffenderAlias> offenderAliases;

    @JsonIgnore
    public List<String> getOffenceCodes() {
        return offenderAliases.stream()
            .map(OffenderAlias::getOffenderBookings)
            .flatMap(Collection::stream)
            .map(OffenderBooking::getOffenceCodes)
            .flatMap(Collection::stream)
            .collect(toList());
    }

    @JsonIgnore
    public List<String> getAlertCodes() {
        return offenderAliases.stream()
            .map(OffenderAlias::getOffenderBookings)
            .flatMap(Collection::stream)
            .map(OffenderBooking::getAlertCodes)
            .flatMap(Collection::stream)
            .collect(toList());
    }

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffenderAlias {

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

        @JsonProperty("bookingNo")
        private String bookingNo;

        @Singular
        @JsonProperty("offenceCodes")
        private Set<String> offenceCodes;

        @Singular
        @JsonProperty("alertCodes")
        private Set<String> alertCodes;
    }
}
