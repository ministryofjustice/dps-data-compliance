package uk.gov.justice.hmpps.datacompliance.client.prisonregister;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;

import java.util.Optional;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class PrisonRegisterClient {

    private static final String OMU_EMAIL_PATH = "/secure/prisons/id/%s/offender-management-unit/email-address";

    private final WebClient webClient;
    private final DataComplianceProperties dataComplianceProperties;

    public PrisonRegisterClient(@Qualifier("authorizedWebClient") final WebClient webClient,
                              final DataComplianceProperties dataComplianceProperties) {
        this.webClient = webClient;
        this.dataComplianceProperties = dataComplianceProperties;
    }

    public Optional<String> retrieveOmuContactEmail(String agencyLocation) {
        final var url = dataComplianceProperties.getPrisonRegisterBaseUrl() +
            format(OMU_EMAIL_PATH, agencyLocation);

        log.debug("Executing a request to '{}' to retrieve the contact email for OMU '{}'", url, agencyLocation);

        return Optional.ofNullable(webClient.get()
            .uri(url)
            .retrieve()
            .onStatus(NOT_FOUND::equals, ignored -> OmuNotFound(agencyLocation))
            .bodyToMono(String.class)
            .block(dataComplianceProperties.getPrisonRegisterTimeout()));
    }

    @NotNull
    private Mono<Throwable> OmuNotFound(String agencyLocation) {
        log.info("Contact details for OMU '{} not found.", agencyLocation);
        return Mono.empty();
    }

}
