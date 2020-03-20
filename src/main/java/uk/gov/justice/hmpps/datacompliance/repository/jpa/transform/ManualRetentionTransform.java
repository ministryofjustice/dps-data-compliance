package uk.gov.justice.hmpps.datacompliance.repository.jpa.transform;

import lombok.NoArgsConstructor;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReason;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ManualRetentionTransform {

    public static ManualRetention transform(
            final uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetention manualRetention) {

        return ManualRetention.builder()
                .offenderNo(manualRetention.getOffenderNo())
                .modifiedDateTime(manualRetention.getRetentionDateTime())
                .userId(manualRetention.getUserId())
                .retentionReasons(manualRetention.getManualRetentionReasons().stream()
                        .map(ManualRetentionTransform::transform)
                        .collect(toList()))
                .build();
    }

    public static ManualRetentionReason transform(
            final uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetentionReason reason) {
        return ManualRetentionReason.builder()
                .reasonCode(ManualRetentionReasonCode.valueOf(reason.getRetentionReasonCodeId().getRetentionReasonCodeId().name()))
                .reasonDetails(reason.getReasonDetails())
                .build();
    }
}
