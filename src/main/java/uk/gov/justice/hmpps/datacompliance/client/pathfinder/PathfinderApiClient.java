package uk.gov.justice.hmpps.datacompliance.client.pathfinder;

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
public class PathfinderApiClient {

    private static final String PATHFINDER_PATH = "/pathfinder/offender/%s";

    private final WebClient webClient;
    private final DataComplianceProperties dataComplianceProperties;

    public PathfinderApiClient(@Qualifier("authorizedWebClient") final WebClient webClient,
                               final DataComplianceProperties dataComplianceProperties) {
        this.webClient = webClient;
        this.dataComplianceProperties = dataComplianceProperties;
    }

    public boolean isReferredToPathfinder(final OffenderNumber offenderNumber) {
        final var url = dataComplianceProperties.getPathfinderApiBaseUrl() +
            format(PATHFINDER_PATH, offenderNumber.getOffenderNumber());

        log.debug("Executing a path finder check to {} for offender '{}'", url, offenderNumber.getOffenderNumber());

        final var response = webClient.get()
            .uri(url)
            .retrieve()

            // Not found should not generate an exception:
            .onStatus(NOT_FOUND::equals, ignored -> Mono.empty())

            .toBodilessEntity()
            .block(dataComplianceProperties.getPathfinderApiTimeout());

        final HttpStatus statusCode = requireNonNull(response).getStatusCode();
        log.debug("Received response for request to {} for offender '{}'. Status code: '{}'", url, offenderNumber.getOffenderNumber(), statusCode.value());
        return statusCode.is2xxSuccessful();
    }
}
