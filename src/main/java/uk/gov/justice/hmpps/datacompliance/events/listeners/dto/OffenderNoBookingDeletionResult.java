package uk.gov.justice.hmpps.datacompliance.events.listeners.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenderNoBookingDeletionResult {

    @JsonProperty("batchId")
    private Long batchId;

    @Singular
    @JsonProperty("offenders")
    private List<Offender> Offenders;


    @Getter
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString(exclude = {"firstName", "middleName", "lastName", "birthDate"})
    public static class Offender {

        @JsonProperty("offenderIdDisplay")
        private String offenderIdDisplay;

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

        @JsonProperty("deletionDateTime")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime deletionDateTime;

        @Singular
        @JsonProperty("offenderAliases")
        private List<OffenderAlias> offenderAliases;


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
    }

}

