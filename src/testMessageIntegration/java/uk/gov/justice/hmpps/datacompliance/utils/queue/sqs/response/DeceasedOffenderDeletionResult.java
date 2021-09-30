package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DeceasedOffenderDeletionResult {

    private Long batchId;

    @Singular
    private List<DeceasedOffender> deceasedOffenders;


    @Getter
    @Builder
    public static class DeceasedOffender {

        private String offenderIdDisplay;

        private String firstName;

        private String middleName;

        private String lastName;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate birthDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate deceasedDate;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime deletionDateTime;

        @JsonProperty("agencyLocationId")
        private String agencyLocationId;

        @Singular
        private List<OffenderAlias> offenderAliases;


    }

    @Getter
    @Builder
    public static class OffenderAlias {

        private Long offenderId;

        @Singular
        @JsonProperty("offenderBookIds")
        private List<Long> offenderBookIds;
    }


}

