package uk.gov.justice.hmpps.datacompliance.services.audit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.DestructionLog;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.RetainedOffender;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit.DestructionLogRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit.RetainedOffenderRepository;
import uk.gov.justice.hmpps.datacompliance.web.dto.DestructionLogResponse;
import uk.gov.justice.hmpps.datacompliance.web.dto.RetainedOffenderResponse;

import javax.transaction.Transactional;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class AuditService {


    private final DestructionLogRepository destructionLogRepository;
    private final RetainedOffenderRepository retainedOffenderRepository;


    public Page<DestructionLogResponse> retrieveDestructionLog(final Pageable pageable) {
        return destructionLogRepository.findDestructionLog(pageable)
            .map(this::transform);
    }

    public Page<RetainedOffenderResponse> retrieveRetainedOffenderDuplicates(final Pageable pageable) {
        return retainedOffenderRepository.findRetainedOffenderDuplicates(pageable)
            .map(this::transform);
    }

    public Page<RetainedOffenderResponse> retrieveRetainedOffenders(final Pageable pageable) {
        return retainedOffenderRepository.findRetainedOffenders(pageable)
            .map(this::transform);
    }

    private DestructionLogResponse transform(final DestructionLog destructionLog) {
        return DestructionLogResponse.builder()
            .nomisId(destructionLog.getOffenderNumber().getOffenderNumber())
            .firstName(destructionLog.getFirstName())
            .middleName(destructionLog.getMiddleName())
            .lastName(destructionLog.getLastName())
            .authorisationOfDestruction(destructionLog.getAuthorisationOfDestruction())
            .typeOfRecordDestroyed(destructionLog.getTypeOfRecordDestroyed())
            .dateOfBirth(destructionLog.getDateOfBirth())
            .lastKnownOmu(destructionLog.getLastKnownOmu())
            .destructionDate(destructionLog.getDestructionDate())
            .methodOfDestruction(destructionLog.getMethodOfDestruction())
            .build();
    }

    private RetainedOffenderResponse transform(final RetainedOffender retainedOffender) {
        return RetainedOffenderResponse.builder()
            .nomisId(retainedOffender.getOffenderNumber().getOffenderNumber())
            .firstName(retainedOffender.getFirstName())
            .middleName(retainedOffender.getMiddleName())
            .lastName(retainedOffender.getLastName())
            .dateOfBirth(retainedOffender.getDateOfBirth())
            .lastKnownOmu(retainedOffender.getLastKnownOmu())
            .retentionReasons(String.join(" & ", retainedOffender.getPositiveRetentionChecks()))
            .build();
    }
}
