package uk.gov.justice.hmpps.datacompliance.utils.sqs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
public class SqsMessage extends Sqs {

    private String eventType;
    private Sqs sqsMessage;

    public SqsMessage withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public SqsMessage withDeletionGranted(String offenderIdDisplay, Long referralId) {
        this.sqsMessage = OffenderDeletionGranted.builder()
            .offenderIdDisplay(offenderIdDisplay)
            .referralId(referralId)
            .build();

        return this;
    }

    public String asJson() {
        try {
            return getObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}


@Builder
@Getter
@JsonInclude(NON_NULL)
class OffenderDeletionGranted extends Sqs {

    private String offenderIdDisplay;

    private Long referralId;

    @Singular
    private Set<Long> offenderIds;

    @Singular
    private Set<Long> offenderBookIds;
}