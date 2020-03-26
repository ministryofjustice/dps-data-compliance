package uk.gov.justice.hmpps.datacompliance.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetentionReason;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonCode;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonCode.Code;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ManualRetentionRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.RetentionReasonCodeRepository;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonCode;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonDisplayName;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionRequest;

import javax.annotation.Nullable;
import javax.persistence.OptimisticLockException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@Transactional
@AllArgsConstructor
public class OffenderRetentionService {

    private final TimeSource timeSource;
    private final UserSecurityUtils userSecurityUtils;
    private final ManualRetentionRepository manualRetentionRepository;
    private final RetentionReasonCodeRepository retentionReasonCodeRepository;

    public List<ManualRetentionReasonDisplayName> getRetentionReasons() {
        return stream(retentionReasonCodeRepository.findAll().spliterator(), false)
                .map(this::transform)
                .distinct()
                .collect(toList());
    }

    public Optional<ManualRetention> findManualOffenderRetention(final OffenderNumber offenderNumber) {
        return manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc(offenderNumber.getOffenderNumber());
    }

    /**
     * Updates / creates a retention record for the offender.
     *
     * Uses optimistic locking to ensure the client is updating the latest version of the record.
     *
     * @param ifMatchValue Uses the 'If-Match' header (for an update) to check the client has the latest version of the record.
     * @return Returns the previous version of the record if an update, or empty if a creation.
     */
    public Optional<ManualRetention> updateManualOffenderRetention(final OffenderNumber offenderNumber,
                                                                   final ManualRetentionRequest manualRetentionRequest,
                                                                   @Nullable final String ifMatchValue) {

        final var existingRecord = findManualOffenderRetention(offenderNumber);

        existingRecord.ifPresent(record -> checkForConflict(record, ifMatchValue));

        final var versionToPersist = existingRecord
                .map(ManualRetention::getRetentionVersion)
                .map(version -> version + 1)
                .orElse(0);

        manualRetentionRepository.save(generateRecordToPersist(offenderNumber, manualRetentionRequest, versionToPersist));

        return existingRecord;
    }

    public String getETag(final ManualRetention manualRetention) {
        return String.valueOf(manualRetention.getRetentionVersion());
    }

    private void checkForConflict(final ManualRetention existingRecord, @Nullable final String ifMatchValue) {

        if (isEmpty(ifMatchValue)) {
            throw new HttpClientErrorException(BAD_REQUEST, "Must provide 'If-Match' header");
        }

        if (!Objects.equals(getETag(existingRecord), ifMatchValue.replaceAll("\"", ""))) {
            throw new OptimisticLockException("Attempting to update an old version of the retention record");
        }
    }

    private ManualRetention generateRecordToPersist(final OffenderNumber offenderNumber,
                                                    final ManualRetentionRequest manualRetentionRequest,
                                                    final int version) {

        final var recordToPersist = ManualRetention.builder()
                .offenderNo(offenderNumber.getOffenderNumber())
                .retentionDateTime(timeSource.nowAsLocalDateTime())
                .userId(userSecurityUtils.getCurrentUsername()
                        .orElseThrow(() -> new IllegalStateException("Cannot retrieve username from request")))
                .retentionVersion(version)
                .build();

        manualRetentionRequest.getRetentionReasons().stream()
                .map(this::transform)
                .forEach(recordToPersist::addManualRetentionReason);

        return recordToPersist;
    }

    private ManualRetentionReason transform(final uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReason reason) {

        return ManualRetentionReason.builder()
                .retentionReasonCodeId(
                        retentionReasonCodeRepository.findById(Code.valueOf(reason.getReasonCode().name()))
                                .orElseThrow())
                .reasonDetails(reason.getReasonDetails())
                .build();
    }

    private ManualRetentionReasonDisplayName transform(final RetentionReasonCode reason) {
        return ManualRetentionReasonDisplayName.builder()
                .reasonCode(ManualRetentionReasonCode.valueOf(reason.getRetentionReasonCodeId().name()))
                .displayName(reason.getDisplayName())
                .allowReasonDetails(reason.getAllowReasonDetails())
                .displayOrder(reason.getDisplayOrder())
                .build();
    }
}
