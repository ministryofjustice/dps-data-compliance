package uk.gov.justice.hmpps.datacompliance.services.audit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.DestructionLog;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit.AuditRepository;
import uk.gov.justice.hmpps.datacompliance.web.dto.DestructionLogResponse;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class AuditService {


    private final AuditRepository auditRepository;


    public List<DestructionLogResponse> retrieveDestructionLog(){
       return auditRepository.findDestructionLog().stream()
           .map(this::transform)
           .collect(toList());
    }

    public List<DestructionLogResponse> retrieveDestructionLog(LocalDateTime fromDateTime, LocalDateTime toDateTime){
        return auditRepository.findDestructionLogBetweenDates(fromDateTime, toDateTime)
            .stream()
            .map(this::transform)
            .collect(toList());
    }

    private DestructionLogResponse transform(DestructionLog destructionLog) {
        return DestructionLogResponse.builder()
            .nomisId(destructionLog.getOffenderNumber().getOffenderNumber())
            .firstName(destructionLog.getFirstName())
            .middleName(destructionLog.getMiddleName())
            .lastName(destructionLog.getLastName())
            .authorisationOfDestruction(destructionLog.getAuthorisationOfDestruction())
            .typeOfRecordDestroyed(destructionLog.getTypeOfRecordDestroyed())
            .dateOfBirth(destructionLog.getDateOfBirth())
            .destructionDate(destructionLog.getDestructionDate())
            .methodOfDestruction(destructionLog.getMethodOfDestruction())
            .build();
    }
}
