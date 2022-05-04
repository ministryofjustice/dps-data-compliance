package uk.gov.justice.hmpps.datacompliance.services.ual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.hmpps.datacompliance.dto.UalOffender;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.services.ual.IdentifierValidation.ChecksumComponents;
import uk.gov.justice.hmpps.datacompliance.utils.ReportUtility;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.util.StringUtils.hasText;
import static uk.gov.justice.hmpps.datacompliance.services.ual.IdentifierValidation.getValidCroComponents;
import static uk.gov.justice.hmpps.datacompliance.services.ual.IdentifierValidation.getValidPncComponents;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UalReportService {

    private final ReportUtility reportUtility;
    private final OffenderUalRepository offenderUalRepository;
    private final TimeSource timeSource;
    private final UserSecurityUtils userSecurityUtils;

    public List<UalOffenderResponse> getUalOffenders() {
        return transform(offenderUalRepository.findAll());
    }


    public Optional<List<Long>> updateReport(final MultipartFile report) {

        final var offendersUal = reportUtility.parseFromUalReport(report);

        if (offendersUal.isEmpty()) return Optional.empty();

        offenderUalRepository.deleteAll();

        final var updatedReportIds = transform(offendersUal).stream()
            .map(entity -> offenderUalRepository.save(entity).getOffenderUalId())
            .collect(toList());

        return Optional.of(updatedReportIds);
    }


    private List<OffenderUalEntity> transform(final List<UalOffender> offendersUal) {

        final var userId = userSecurityUtils.getCurrentUsername()
            .orElseThrow(() -> new IllegalStateException("Cannot retrieve username from request"));

        final var now = timeSource.nowAsLocalDateTime();

        return offendersUal.stream().map(ualOffender -> OffenderUalEntity
                .builder()
                .offenderNo(ualOffender.getNomsId())
                .offenderBookingNo(ualOffender.getPrisonNumber())
                .offenderPnc(getFormattedPncNumberFrom(ualOffender.getCroPnc()))
                .offenderCro(getFormattedCroNumberFrom(ualOffender.getCroPnc()))
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
                .pnc(offenderUalEntity.getOffenderPnc())
                .cro(offenderUalEntity.getOffenderCro())
                .firstNames(offenderUalEntity.getFirstNames())
                .familyName(offenderUalEntity.getLastName())
                .indexOffenceDescription(offenderUalEntity.getOffenceDescription())
                .build())
            .collect(toList());
    }

    private String getFormattedCroNumberFrom(final String croPnc) {
        if (!hasText(croPnc)) return null;

        return getValidCroComponents(croPnc)
            .map(this::formatChecksumComponentsWithNoLeadingZeros)
            .orElse(null);
    }


    private String getFormattedPncNumberFrom(final String croPnc) {
        if (!hasText(croPnc)) return null;

        return getValidPncComponents(croPnc)
            .map(this::formatChecksumComponentsWithNoLeadingZeros)
            .orElse(null);
    }

    private String formatChecksumComponentsWithNoLeadingZeros(final ChecksumComponents components) {
        return components.getYear() + "/" + parseInt(components.getSerial()) + components.getChecksum();
    }

}
