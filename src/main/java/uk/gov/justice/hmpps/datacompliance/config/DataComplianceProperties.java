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

    private final String prisonApiBaseUrl;
    private final int prisonApiOffenderIdsIterationThreads;
    private final long prisonApiOffenderIdsLimit;
    private final long prisonApiOffenderIdsInitialOffset;
    private final Long prisonApiOffenderIdsTotalPages;
    private final String pathfinderApiBaseUrl;
    private final Duration pathfinderApiTimeout;
    private final String communityApiBaseUrl;
    private final Duration communityApiTimeout;
    private final String prisonRegisterBaseUrl;
    private final Duration prisonRegisterTimeout;
    private final boolean deletionGrantEnabled;
    private final boolean reviewRequired;
    private final boolean imageRecognitionDeletionEnabled;
    private final boolean imageDuplicateCheckEnabled;
    private final boolean idDataDuplicateCheckEnabled;  // Duplicate check based only on IDs (PNC, CRO, LIDS numbers)
    private final boolean databaseDataDuplicateCheckEnabled;  // Duplicate check based on points based similarity SQL query on NOMIS database
    private final boolean analyticalPlatformDataDuplicateCheckEnabled;  // Duplicate check based on Analytical Platform's Apache Spark deduplication solution (Splink)
    private final boolean falsePositiveDuplicateCheckEnabled; // Enable to run verification when only one check method flags up a duplicate
    private final boolean mappaCheckEnabled; // Check Delius (via community api) for MAPPA
    private final double falsePositiveDuplicateImageSimilarityThreshold;
    private final int falsePositiveDuplicateRequiredImageCount;

    public DataComplianceProperties(@Value("${prison.api.base.url}") @URL final String prisonApiBaseUrl,
                                    @Value("${prison.api.offender.ids.iteration.threads:1}") final int prisonApiOffenderIdsIterationThreads,
                                    @Value("${prison.api.offender.ids.page.limit:100}") final long prisonApiOffenderIdsLimit,
                                    @Value("${prison.api.offender.ids.initial.offset:0}") final long prisonApiOffenderIdsInitialOffset,
                                    @Value("${prison.api.offender.ids.total.pages:#{null}}") final Long prisonApiOffenderIdsTotalPages,
                                    @Value("${pathfinder.api.base.url}") @URL final String pathfinderApiBaseUrl,
                                    @Value("${pathfinder.api.timeout:5s}") final Duration pathfinderApiTimeout,
                                    @Value("${community.api.base.url}") @URL final String communityApiBaseUrl,
                                    @Value("${community.api.timeout:5s}") final Duration communityApiTimeout,
                                    @Value("${prison.register.base.url}") @URL final String prisonRegisterBaseUrl,
                                    @Value("${prison.register.timeout:5s}") final Duration prisonRegisterTimeout,
                                    @Value("${deletion.grant.enabled}") final boolean deletionGrantEnabled,
                                    @Value("${offender.deletion.review.required}") final boolean reviewRequired,
                                    @Value("${image.recognition.deletion.enabled}") final boolean imageRecognitionDeletionEnabled,
                                    @Value("${offender.retention.image.duplicate.check.enabled}") final boolean imageDuplicateCheckEnabled,
                                    @Value("${offender.retention.data.duplicate.id.check.enabled}") final boolean idDataDuplicateCheckEnabled,
                                    @Value("${offender.retention.data.duplicate.db.check.enabled}") final boolean databaseDataDuplicateCheckEnabled,
                                    @Value("${offender.retention.data.duplicate.ap.check.enabled}") final boolean analyticalPlatformDataDuplicateCheckEnabled,
                                    @Value("${offender.retention.false.positive.duplicate.check.enabled}") final boolean falsePositiveDuplicateCheckEnabled,
                                    @Value("${offender.retention.mappa.check.enabled}") final boolean mappaCheckEnabled,
                                    @Value("${offender.retention.false.positive.duplicate.image.similarity.threshold:80}") final double falsePositiveDuplicateImageSimilarityThreshold,
                                    @Value("${offender.retention.false.positive.duplicate.required.image.count:1}") final int falsePositiveDuplicateRequiredImageCount) {

        log.info("Image upload - number of threads: {}", prisonApiOffenderIdsIterationThreads);
        log.info("Image upload - page limit: {}", prisonApiOffenderIdsLimit);
        log.info("Image upload - initial offset: {}", prisonApiOffenderIdsInitialOffset);
        log.info("Image upload - total pages: {}", prisonApiOffenderIdsTotalPages);
        log.info("Deletion grant enabled: {}", deletionGrantEnabled);
        log.info("Deletion review required: {}", reviewRequired);
        log.info("Image Recognition deletion enabled: {}", imageRecognitionDeletionEnabled);
        log.info("Image Duplicate check enabled: {}", imageDuplicateCheckEnabled);
        log.info("Data Duplicate - ID check enabled: {}", idDataDuplicateCheckEnabled);
        log.info("Data Duplicate - SQL query check enabled: {}", databaseDataDuplicateCheckEnabled);
        log.info("Data Duplicate - Analytical Platform check enabled: {}", analyticalPlatformDataDuplicateCheckEnabled);
        log.info("Data Duplicate - False positive check enabled: {}", falsePositiveDuplicateCheckEnabled);
        log.info("MAPPA check enabled: {}", mappaCheckEnabled);

        this.prisonApiBaseUrl = prisonApiBaseUrl;
        this.prisonApiOffenderIdsIterationThreads = prisonApiOffenderIdsIterationThreads;
        this.prisonApiOffenderIdsLimit = prisonApiOffenderIdsLimit;
        this.prisonApiOffenderIdsInitialOffset = prisonApiOffenderIdsInitialOffset;
        this.prisonApiOffenderIdsTotalPages = prisonApiOffenderIdsTotalPages;
        this.pathfinderApiBaseUrl = pathfinderApiBaseUrl;
        this.pathfinderApiTimeout = pathfinderApiTimeout;
        this.communityApiBaseUrl = communityApiBaseUrl;
        this.communityApiTimeout = communityApiTimeout;
        this.prisonRegisterBaseUrl = prisonRegisterBaseUrl;
        this.prisonRegisterTimeout = prisonRegisterTimeout;
        this.deletionGrantEnabled = deletionGrantEnabled;
        this.reviewRequired = reviewRequired;
        this.imageRecognitionDeletionEnabled = imageRecognitionDeletionEnabled;
        this.imageDuplicateCheckEnabled = imageDuplicateCheckEnabled;
        this.idDataDuplicateCheckEnabled = idDataDuplicateCheckEnabled;
        this.databaseDataDuplicateCheckEnabled = databaseDataDuplicateCheckEnabled;
        this.analyticalPlatformDataDuplicateCheckEnabled = analyticalPlatformDataDuplicateCheckEnabled;
        this.falsePositiveDuplicateCheckEnabled = falsePositiveDuplicateCheckEnabled;
        this.mappaCheckEnabled = mappaCheckEnabled;
        this.falsePositiveDuplicateImageSimilarityThreshold = falsePositiveDuplicateImageSimilarityThreshold;
        this.falsePositiveDuplicateRequiredImageCount = falsePositiveDuplicateRequiredImageCount;
    }

    public Optional<Long> getOffenderIdsTotalPages() {
        return Optional.ofNullable(prisonApiOffenderIdsTotalPages);
    }

    @Bean
    public TimeSource timeSource() {
        return TimeSource.systemUtc();
    }
}
