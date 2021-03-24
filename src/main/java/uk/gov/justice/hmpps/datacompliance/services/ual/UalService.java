package uk.gov.justice.hmpps.datacompliance.services.ual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.utils.ReportUtility;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UalService {

    private final ReportUtility reportUtility;
    private final OffenderUalRepository offenderUalRepository;
    private final TimeSource timeSource;
    private final UserSecurityUtils userSecurityUtils;

    public List<UalOffenderResponse> getUalOffenders() {
        return transform(offenderUalRepository.findAll());
    }


    public Optional<List<Long>> updateReport(final MultipartFile report) {

        final var offendersUal = reportUtility.parseFromUalReport(report);

        if (offendersUal.isEmpty()) {
            return Optional.empty();
        }

        final var updatedReportIds = transform(offendersUal).stream()
            .map(this::update)
            .map(OffenderUalEntity::getOffenderUalId)
            .collect(toList());

        deleteWithdrawnOffenders(updatedReportIds);

        return Optional.of(updatedReportIds);
    }


    public boolean isUnlawfullyAtLarge(OffenderNumber offenderNumber) {
        return offenderUalRepository.findOneByOffenderNo(offenderNumber.getOffenderNumber()).isPresent();
    }

    private OffenderUalEntity update(final OffenderUalEntity offenderUalEntity) {

        final var existingEntity = offenderUalEntity.hasOffenderNumber() ?
            offenderUalRepository.findOneByOffenderNo(offenderUalEntity.getOffenderNo())
            : offenderUalRepository.findOneByOffenderBookingNoAndOffenderCroPncAndFirstNamesAndLastName(offenderUalEntity.getOffenderBookingNo(), offenderUalEntity.getOffenderCroPnc(), offenderUalEntity.getFirstNames(), offenderUalEntity.getLastName());

        existingEntity.ifPresent(existingMatch -> offenderUalEntity.setOffenderUalId(existingMatch.getOffenderUalId()));

        return offenderUalRepository.save(offenderUalEntity);
    }

    private void deleteWithdrawnOffenders(final List<Long> updatedReportIds) {
        offenderUalRepository.deleteByOffenderUalIdNotIn(updatedReportIds);
    }


    private List<OffenderUalEntity> transform(final List<UalOffender> offendersUal) {

        final var userId = userSecurityUtils.getCurrentUsername()
            .orElseThrow(() -> new IllegalStateException("Cannot retrieve username from request"));

        final var now = timeSource.nowAsLocalDateTime();

        return offendersUal.stream().map(ualOffender -> OffenderUalEntity
            .builder()
            .offenderNo(ualOffender.getNomsId())
            .offenderBookingNo(ualOffender.getPrisonNumber())
            .offenderCroPnc(ualOffender.getCroPnc())
            .firstNames(ualOffender.getFirstNames())
            .lastName(ualOffender.getFamilyName())
            .offenceDescription(ualOffender.getIndexOffenceDescription())
            .userId(userId)
            .uploadDateTime(now)
            .build())
            .collect(toList());
    }

    private List<UalOffenderResponse> transform(final Iterable<OffenderUalEntity> offenderUalEntities) {
        return stream(offenderUalEntities.spliterator(), false)
            .map(offenderUalEntity -> UalOffenderResponse
                .builder()
                .nomsId(offenderUalEntity.getOffenderNo())
                .prisonNumber(offenderUalEntity.getOffenderBookingNo())
                .croPnc(offenderUalEntity.getOffenderCroPnc())
                .firstNames(offenderUalEntity.getFirstNames())
                .familyName(offenderUalEntity.getLastName())
                .indexOffenceDescription(offenderUalEntity.getOffenceDescription())
                .build())
            .collect(toList());
    }

}
