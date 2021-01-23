package uk.gov.justice.hmpps.datacompliance.utils.sqs.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@Getter
@Builder
@JsonInclude(NON_NULL)
public class OffenderDeletionComplete {

    private String offenderIdDisplay;

    @Singular
    private List<OffenderWithBookings> offenders;

    @Getter
    @Builder
    public static class OffenderWithBookings {

        private Long offenderId;

        @Singular
        private List<Booking> bookings;
    }

    @Getter
    @Builder
    public static class Booking {

        private Long offenderBookId;
    }
}

