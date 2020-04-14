package uk.gov.justice.hmpps.datacompliance.config;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Getter
@Builder
@Validated
@Configuration
@EnableScheduling
public class DataComplianceProperties {

    private final String elite2ApiBaseUrl;
    private final int elite2ApiOffenderIdsIterationThreads;
    private final long elite2ApiOffenderIdsLimit;
    private final long elite2ApiOffenderIdsInitialOffset;
    private final Long elite2ApiOffenderIdsTotalPages;
    private final String pathfinderApiBaseUrl;
    private final Duration pathfinderApiTimeout;

    public DataComplianceProperties(@Value("${elite2.api.base.url}") @URL final String elite2ApiBaseUrl,
                                    @Value("${elite2.api.offender.ids.iteration.threads:1}") final int elite2ApiOffenderIdsIterationThreads,
                                    @Value("${elite2.api.offender.ids.page.limit:100}") final long elite2ApiOffenderIdsLimit,
                                    @Value("${elite2.api.offender.ids.initial.offset:0}") final long elite2ApiOffenderIdsInitialOffset,
                                    @Value("${elite2.api.offender.ids.total.pages:#{null}}") final Long elite2ApiOffenderIdsTotalPages,
                                    @Value("${pathfinder.api.base.url}") @URL final String pathfinderApiBaseUrl,
                                    @Value("${pathfinder.api.timeout:5s}") final Duration pathfinderApiTimeout) {

        log.info("Image upload - number of threads: {}", elite2ApiOffenderIdsIterationThreads);
        log.info("Image upload - page limit: {}", elite2ApiOffenderIdsLimit);
        log.info("Image upload - initial offset: {}", elite2ApiOffenderIdsInitialOffset);
        log.info("Image upload - total pages: {}", elite2ApiOffenderIdsTotalPages);

        this.elite2ApiBaseUrl = elite2ApiBaseUrl;
        this.elite2ApiOffenderIdsIterationThreads = elite2ApiOffenderIdsIterationThreads;
        this.elite2ApiOffenderIdsLimit = elite2ApiOffenderIdsLimit;
        this.elite2ApiOffenderIdsInitialOffset = elite2ApiOffenderIdsInitialOffset;
        this.elite2ApiOffenderIdsTotalPages = elite2ApiOffenderIdsTotalPages;
        this.pathfinderApiBaseUrl = pathfinderApiBaseUrl;
        this.pathfinderApiTimeout = pathfinderApiTimeout;
    }

    public Optional<Long> getOffenderIdsTotalPages() {
        return Optional.ofNullable(elite2ApiOffenderIdsTotalPages);
    }

    @Bean
    public TimeSource timeSource() {
        return TimeSource.systemUtc();
    }
}
