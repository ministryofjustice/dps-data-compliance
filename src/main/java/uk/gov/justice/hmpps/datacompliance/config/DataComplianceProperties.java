package uk.gov.justice.hmpps.datacompliance.config;

import lombok.Getter;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@Validated
@Getter
public class DataComplianceProperties {

    private final String elite2ApiBaseUrl;

    public DataComplianceProperties(@Value("${elite2.api.base.url}") @URL final String elite2ApiBaseUrl) {
        this.elite2ApiBaseUrl = elite2ApiBaseUrl;
    }
}
