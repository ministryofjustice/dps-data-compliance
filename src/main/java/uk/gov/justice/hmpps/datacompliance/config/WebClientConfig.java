package uk.gov.justice.hmpps.datacompliance.config;

import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Validated
@Configuration
public class WebClientConfig {

    private final String hmppsAuthBaseUrl;
    private final String prisonApiBaseUrl;
    private final String pathfinderApiBaseUrl;

    public WebClientConfig(@Value("${hmpps.auth.base.url}") @URL final String hmppsAuthBaseUrl,
                           @Value("${prison.api.base.url}") @URL final String prisonApiBaseUrl,
                           @Value("${pathfinder.api.base.url}") @URL final String pathfinderApiBaseUrl) {
        this.hmppsAuthBaseUrl = hmppsAuthBaseUrl;
        this.prisonApiBaseUrl = prisonApiBaseUrl;
        this.pathfinderApiBaseUrl = pathfinderApiBaseUrl;
    }

    @Bean(name = "hmppsAuthHealthWebClient")
    WebClient hmppsAuthHealthWebClient() {
        return WebClient.create(hmppsAuthBaseUrl);
    }

    @Bean(name = "prisonApiHealthWebClient")
    WebClient prisonApiHealthWebClient() {
        return WebClient.create(prisonApiBaseUrl);
    }

    @Bean(name = "pathfinderApiHealthWebClient")
    WebClient pathfinderApiHealthWebClient() {
        return WebClient.create(pathfinderApiBaseUrl);
    }

    @Bean(name = "authorizedWebClient")
    WebClient authorizedWebClient(final OAuth2AuthorizedClientManager authorizedClientManager) {

        var oauth2Client = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        oauth2Client.setDefaultClientRegistrationId("dps-data-compliance");

        return WebClient.builder()
                .apply(oauth2Client.oauth2Configuration())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer  -> configurer.defaultCodecs().maxInMemorySize(-1))
                        .build())
                .build();
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            final ClientRegistrationRepository clientRegistrationRepository,
            final OAuth2AuthorizedClientService clientService) {

        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

}
