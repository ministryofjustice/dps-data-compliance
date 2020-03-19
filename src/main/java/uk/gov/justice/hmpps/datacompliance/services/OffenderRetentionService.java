package uk.gov.justice.hmpps.datacompliance.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ManualRetentionRepository;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReason;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode;

import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class OffenderRetentionService {

    private final ManualRetentionRepository manualRetentionRepository;

    public Optional<ManualRetention> findManualOffenderRetention(final OffenderNumber offenderNumber) {
        return manualRetentionRepository.findFirstByOffenderNoOrderByRetentionDateTimeDesc(offenderNumber.getOffenderNumber())
                .map(this::transform);
    }

    private ManualRetention transform(
            final uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetention manualRetention) {

        return ManualRetention.builder()
                .offenderNo(manualRetention.getOffenderNo())
                .modifiedDateTime(manualRetention.getRetentionDateTime())
                .staffId(manualRetention.getStaffId())
                .retentionReasons(manualRetention.getManualRetentionReasons().stream()
                        .map(this::transform)
                        .collect(toList()))
                .build();
    }

    private ManualRetentionReason transform(
            final uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetentionReason reason) {
        return ManualRetentionReason.builder()
                .reasonCode(ManualRetentionReasonCode.valueOf(reason.getRetentionReasonCodeId().getRetentionReasonCodeId().name()))
                .reasonDetails(reason.getReasonDetails())
                .build();
    }
}
