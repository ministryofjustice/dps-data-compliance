package uk.gov.justice.hmpps.datacompliance.utils.web.request;

public class RequestFactory {

    public static ManualRetentionRequest forManualRetentionRequest(ManualRetentionReasonCode reasonCode) {
        return ManualRetentionRequest.builder()
            .retentionReason(ManualRetentionReason.builder()
                .reasonCode(reasonCode)
                .reasonDetails("High profile for some reason")
                .build())
            .build();
    }
}
