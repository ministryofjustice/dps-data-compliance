package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@Inheritance
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorColumn(name = "REASON_CODE")
@ToString(exclude = {"referralResolution"}) // to avoid circular reference
@EqualsAndHashCode(of = {"retentionReasonId"})
@Table(name = "RETENTION_REASON")
public abstract class RetentionReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RETENTION_REASON_ID", nullable = false)
    private Long retentionReasonId;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "RESOLUTION_ID", nullable = false)
    private ReferralResolution referralResolution;

    @NotNull
    @Column(name = "REASON_CODE", nullable = false, insertable = false, updatable = false)
    private String reasonCode;
}
