package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetentionReason;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.RetentionReasonCode.Code.HIGH_PROFILE;

@Transactional
class ManualRetentionRepositoryTest extends IntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private ManualRetentionRepository manualRetentionRepository;

    @Autowired
    private RetentionReasonCodeRepository reasonCodeRepository;

    @Test
    @Sql("classpath:seed.data/retention_reason_code.sql")
    void persistManualRetentionAndRetrieveById() {

        final var manualRetention = ManualRetention.builder()
                .offenderNo("A1234BC")
                .userId("user1")
                .retentionDateTime(NOW)
                .retentionVersion(1)
                .build();

        manualRetention.addManualRetentionReason(ManualRetentionReason.builder()
                .retentionReasonCodeId(reasonCodeRepository.findById(HIGH_PROFILE).orElseThrow())
                .reasonDetails("High profile for some reason")
                .build());

        manualRetentionRepository.save(manualRetention);

        final var retrievedEntity = manualRetentionRepository.findById(manualRetention.getManualRetentionId()).orElseThrow();

        assertThat(retrievedEntity.getOffenderNo()).isEqualTo("A1234BC");
        assertThat(retrievedEntity.getUserId()).isEqualTo("user1");
        assertThat(retrievedEntity.getRetentionDateTime()).isEqualTo(NOW);
        assertThat(retrievedEntity.getRetentionVersion()).isEqualTo(1);
        assertThat(retrievedEntity.getManualRetentionReasons()).hasSize(1);
        assertThat(retrievedEntity.getManualRetentionReasons())
                .extracting(r -> r.getRetentionReasonCodeId().getRetentionReasonCodeId())
                .containsExactly(HIGH_PROFILE);
        assertThat(retrievedEntity.getManualRetentionReasons())
                .extracting(ManualRetentionReason::getReasonDetails)
                .containsExactly("High profile for some reason");
    }

    @Test
    @Sql("classpath:seed.data/retention_reason_code.sql")
    @Sql("classpath:seed.data/manual_retention.sql")
    @Sql("classpath:seed.data/manual_retention_reason.sql")
    void findLatestManualRetentionRecordForOffenderNo() {

        final var latestManualRetention = manualRetentionRepository.findFirstByOffenderNoOrderByRetentionVersionDesc("A1234BC").orElseThrow();

        assertThat(latestManualRetention.getOffenderNo()).isEqualTo("A1234BC");
        assertThat(latestManualRetention.getUserId()).isEqualTo("user3");
        assertThat(latestManualRetention.getRetentionDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 3, 1, 2, 3));
        assertThat(latestManualRetention.getRetentionVersion()).isEqualTo(3);
        assertThat(latestManualRetention.getManualRetentionReasons()).hasSize(1);
        assertThat(latestManualRetention.getManualRetentionReasons())
                .extracting(r -> r.getRetentionReasonCodeId().getRetentionReasonCodeId())
                .containsExactly(HIGH_PROFILE);
        assertThat(latestManualRetention.getManualRetentionReasons())
                .extracting(ManualRetentionReason::getReasonDetails)
                .containsExactly("High profile for some reason");
    }
}