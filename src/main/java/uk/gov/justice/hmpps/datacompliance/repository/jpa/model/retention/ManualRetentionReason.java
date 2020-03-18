package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"manualRetentionReasonId"})
@ToString(exclude = "manualRetention") // to avoid circular reference
@Table(name = "MANUAL_RETENTION_REASON")
public class ManualRetentionReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MANUAL_RETENTION_REASON_ID", nullable = false)
    private Long manualRetentionReasonId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "MANUAL_RETENTION_ID", nullable = false)
    private ManualRetention manualRetention;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "RETENTION_REASON_CODE_ID", nullable = false)
    private RetentionReasonCode retentionReasonCodeId;

    @Length(max = 4000)
    @Column(name = "REASON_DETAILS")
    private String reasonDetails;
}
