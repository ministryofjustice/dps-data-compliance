package uk.gov.justice.hmpps.datacompliance.client.communityapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class CommunityApiClient {

    private static final String MAPPA_RISK_PATH = "/secure/offenders/nomsNumber/%s/risk/mappa";

    private final WebClient webClient;
    private final DataComplianceProperties dataComplianceProperties;

    public CommunityApiClient(@Qualifier("authorizedWebClient") final WebClient webClient,
                              final DataComplianceProperties dataComplianceProperties) {
        this.webClient = webClient;
        this.dataComplianceProperties = dataComplianceProperties;
    }

    public boolean isReferredForMappa(final OffenderNumber offenderNumber) {
        final var url = dataComplianceProperties.getCommunityApiBaseUrl() +
            format(MAPPA_RISK_PATH, offenderNumber.getOffenderNumber());

        log.debug("Executing a MAPPA (Multi-Agency Public Protection Arrangements) check to {} for offender '{}'", url, offenderNumber.getOffenderNumber());

        final var response = webClient.get()
            .uri(url)
            .retrieve()
            .onStatus(NOT_FOUND::equals, ignored -> Mono.empty())
            .toBodilessEntity()
            .block(dataComplianceProperties.getCommunityApiTimeout());

        final HttpStatus statusCode = requireNonNull(response).getStatusCode();
        log.debug("Received response for request to {} for offender '{}'. Status code: '{}'", url, offenderNumber.getOffenderNumber(), statusCode.value());
        return statusCode.is2xxSuccessful();
    }
}
