package uk.gov.justice.hmpps.datacompliance.config;

import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.Optional;

@Getter
@Builder
@Validated
@Configuration
@EnableScheduling
public class DataComplianceProperties {

    private final String elite2ApiBaseUrl;
    private final int elite2ApiOffenderIdsIterationThreads;
    private final long elite2ApiOffenderIdsLimit;

    // These properties allow a test to be conducted over
    // a smaller sample of data. Can be removed once we have
    // confidence and a time estimate for running over the entire
    // data set: TODO[GDPR-38]
    private final long elite2ApiOffenderIdsInitialOffset;
    private final Long elite2ApiOffenderIdsTotalPages;

    public DataComplianceProperties(@Value("${elite2.api.base.url}") @URL final String elite2ApiBaseUrl,
                                    @Value("${elite2.api.offender.ids.iteration.threads:1}") final int elite2ApiOffenderIdsIterationThreads,
                                    @Value("${elite2.api.offender.ids.page.limit:100}") final long elite2ApiOffenderIdsLimit,
                                    @Value("${elite2.api.offender.ids.initial.offset:0}") final long elite2ApiOffenderIdsInitialOffset,
                                    @Value("${elite2.api.offender.ids.total.pages:#{null}}") final Long elite2ApiOffenderIdsTotalPages) {
        this.elite2ApiBaseUrl = elite2ApiBaseUrl;
        this.elite2ApiOffenderIdsIterationThreads = elite2ApiOffenderIdsIterationThreads;
        this.elite2ApiOffenderIdsLimit = elite2ApiOffenderIdsLimit;
        this.elite2ApiOffenderIdsInitialOffset = elite2ApiOffenderIdsInitialOffset;
        this.elite2ApiOffenderIdsTotalPages = elite2ApiOffenderIdsTotalPages;
    }

    public Optional<Long> getOffenderIdsTotalPages() {
        return Optional.ofNullable(elite2ApiOffenderIdsTotalPages);
    }

    @Bean
    public TimeSource timeSource() {
        return TimeSource.systemUtc();
    }
}
