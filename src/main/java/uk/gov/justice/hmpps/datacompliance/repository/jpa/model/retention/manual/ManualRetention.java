package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"manualRetentionId"})
@Table(name = "MANUAL_RETENTION")
public class ManualRetention {

    @OneToMany(mappedBy = "manualRetention", cascade = PERSIST, fetch = LAZY)
    private final List<ManualRetentionReason> manualRetentionReasons = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MANUAL_RETENTION_ID", nullable = false)
    private Long manualRetentionId;
    @NotNull
    @Length(max = 10)
    @Column(name = "OFFENDER_NO", nullable = false)
    private String offenderNo;
    @NotNull
    @Column(name = "RETENTION_DATE_TIME", nullable = false)
    private LocalDateTime retentionDateTime;
    @NotNull
    @Column(name = "USER_ID", nullable = false)
    private String userId;
    @NotNull
    @Column(name = "RETENTION_VERSION", nullable = false)
    private Integer retentionVersion;

    public void addManualRetentionReason(final ManualRetentionReason reason) {
        this.manualRetentionReasons.add(reason);
        reason.setManualRetention(this);
    }
}
