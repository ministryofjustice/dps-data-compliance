package uk.gov.justice.hmpps.datacompliance.services.ual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UalService {

    public static final double SIMILARITY_THRESHOLD = 0.93;
    private final OffenderUalRepository offenderUalRepository;

    public boolean isUnlawfullyAtLarge(OffenderToCheck offender) {
        log.info("Conducting a search for offender {} against the unlawfully at large database", offender.getOffenderNumber().getOffenderNumber());
        final var isUal = findUalOffender(offender).isPresent();
        log.info(isUal ? "Match found for Offender {} against the unlawfully at large database" : "No Match found for Offender {} against the unlawfully at large database", offender.getOffenderNumber().getOffenderNumber());
        return isUal;
    }

    public Optional<OffenderUalEntity> findUalOffender(final OffenderToCheck offender) {
        final var ualOffender = offenderUalRepository.findOneByOffenderNoIgnoreCase(offender.getOffenderNumber().getOffenderNumber())
            .or(() -> findOffenderByBooking(offender.getBookingNos())
                .or(() -> findOffenderByPncs(offender.getPncs())
                    .or(() -> findOffenderByCros(offender.getCros()))));

        return ualOffender
            .filter(ualOff -> findLeventienSimilarity(ualOff.getFirstNames(), offender.getFirstNames()) >= SIMILARITY_THRESHOLD)
            .filter(ualOff -> findLeventienSimilarity(ualOff.getLastName(), offender.getLastName()) >= SIMILARITY_THRESHOLD)
            .filter(ualOff -> findJaroWinklerDistanceSimilarity(ualOff.getFirstNames(), offender.getFirstNames()) >= SIMILARITY_THRESHOLD)
            .filter(ualOff -> findJaroWinklerDistanceSimilarity(ualOff.getLastName(), offender.getLastName()) >= SIMILARITY_THRESHOLD);
    }


    private double findLeventienSimilarity(String x, String y) {
        double maxLength = Double.max(x.length(), y.length());
        if (maxLength > 0) {
            return (maxLength - StringUtils.getLevenshteinDistance(x, y)) / maxLength;
        }
        return 1.0;
    }

    private double findJaroWinklerDistanceSimilarity(String x, String y) {
        if (x == null && y == null) {
            return 1.0;
        }
        if (x == null || y == null) {
            return 0.0;
        }
        return StringUtils.getJaroWinklerDistance(x, y);
    }

    private Optional<OffenderUalEntity> findOffenderByBooking(final Set<String> bookingNos) {
        return bookingNos.stream()
            .map(offenderUalRepository::findOneByOffenderBookingNoIgnoreCase)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private Optional<OffenderUalEntity> findOffenderByPncs(Set<String> pncs) {
        return pncs.stream()
            .filter(org.springframework.util.StringUtils::hasText)
            .map(offenderUalRepository::findOneByOffenderPncIgnoreCase)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();

    }

    private Optional<? extends OffenderUalEntity> findOffenderByCros(final Set<String> cros) {
        return cros.stream()
            .filter(org.springframework.util.StringUtils::hasText)
            .map(offenderUalRepository::findOneByOffenderCroIgnoreCase)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

}
