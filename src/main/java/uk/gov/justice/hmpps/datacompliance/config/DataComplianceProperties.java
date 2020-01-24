package uk.gov.justice.hmpps.datacompliance.config;

import lombok.Getter;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@Configuration
@EnableScheduling
public class DataComplianceProperties {

    private final String elite2ApiBaseUrl;
    private final long elite2ApiOffenderIdsLimit;

    public DataComplianceProperties(@Value("${elite2.api.base.url}") @URL final String elite2ApiBaseUrl,
                                    @Value("${elite2.api.offender.ids.page.limit:100}") final long elite2ApiOffenderIdsLimit) {
        this.elite2ApiBaseUrl = elite2ApiBaseUrl;
        this.elite2ApiOffenderIdsLimit = elite2ApiOffenderIdsLimit;
    }
}
