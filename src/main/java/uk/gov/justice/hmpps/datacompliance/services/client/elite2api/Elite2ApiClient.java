package uk.gov.justice.hmpps.datacompliance.services.client.elite2api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;

import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Service
public class Elite2ApiClient {

    private static final String OFFENDER_IDS_PATH = "/api/offenders/ids";

    private final WebClient webClient;
    private final DataComplianceProperties dataComplianceProperties;

    public Elite2ApiClient(@Qualifier("authorizedWebClient") final WebClient webClient,
                           final DataComplianceProperties dataComplianceProperties) {
        this.webClient = webClient;
        this.dataComplianceProperties = dataComplianceProperties;
    }

    public Set<String> getOffenderNumbers(final long offset, final long limit) {
        return getOffenderNumbersFlux(offset, limit).toStream().collect(toSet());
    }

    public Flux<String> getOffenderNumbersFlux(final long offset, final long limit) {

        return webClient.get()
                .uri(dataComplianceProperties.getElite2ApiBaseUrl() + OFFENDER_IDS_PATH)
                .header("Page-Offset", String.valueOf(offset))
                .header("Page-Limit", String.valueOf(limit))
                .attributes(clientRegistrationId("dps-data-compliance"))
                .retrieve()
                .bodyToFlux(OffenderNumber.class)
                .map(response -> requireNonNull(response.getOffenderNumber(),
                        "Response contained null offender numbers"))
                .distinct();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class OffenderNumber {
        @JsonProperty("offenderNumber")
        private String offenderNumber;
    }
}
