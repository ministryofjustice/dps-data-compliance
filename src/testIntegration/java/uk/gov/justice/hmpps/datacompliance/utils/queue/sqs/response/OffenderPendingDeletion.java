package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Getter
@Builder
public class OffenderPendingDeletion {

    private String offenderIdDisplay;

    private Long batchId;

    private String firstName;

    private String middleName;

    private String lastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate birthDate;

    @Singular
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
    public static class OffenderAlias {

        private Long offenderId;

        @Singular
        private List<OffenderBooking> offenderBookings;
    }

    @Getter
    @Builder
    public static class OffenderBooking {

        private Long offenderBookId;

        private String agencyLocationId;

        @Singular
        private Set<String> offenceCodes;

        @Singular
        private Set<String> alertCodes;
    }
}
