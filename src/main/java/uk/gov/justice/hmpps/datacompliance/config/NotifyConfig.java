package uk.gov.justice.hmpps.datacompliance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;

@Configuration
public class NotifyConfig {

    @Bean
    public NotificationClient notifyClient(@Value("${notify.api.key:invalidKey}") String apiKey) {
        return new NotificationClient(apiKey);
    }
}
