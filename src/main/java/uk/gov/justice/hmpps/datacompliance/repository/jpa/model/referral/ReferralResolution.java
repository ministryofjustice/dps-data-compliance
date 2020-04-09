package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"resolutionId"})
@ToString(exclude = "offenderDeletionReferral") // to avoid circular reference
@Table(name = "REFERRAL_RESOLUTION")
public class ReferralResolution {

    public enum ResolutionType {
        RETAINED,
        DELETION_GRANTED,
        DELETED
    }

    // Not using @AllArgsConstructor so that retentionReason
    // does not appear in the builder:
    @Builder
    public ReferralResolution(final Long resolutionId,
                              final OffenderDeletionReferral offenderDeletionReferral,
                              final ReferralResolution.ResolutionType resolutionType,
                              final LocalDateTime resolutionDateTime) {
        this.resolutionId = resolutionId;
        this.offenderDeletionReferral = offenderDeletionReferral;
        this.resolutionType = resolutionType;
        this.resolutionDateTime = resolutionDateTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESOLUTION_ID", nullable = false)
    private Long resolutionId;

    @NotNull
    @OneToOne
    @JoinColumn(name = "REFERRAL_ID", nullable = false)
    private OffenderDeletionReferral offenderDeletionReferral;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "RESOLUTION_TYPE", nullable = false)
    private ReferralResolution.ResolutionType resolutionType;

    @NotNull
    @Column(name = "RESOLUTION_DATE_TIME")
    private LocalDateTime resolutionDateTime;

    @OneToOne(mappedBy = "referralResolution", cascade = CascadeType.PERSIST)
    private RetentionReason retentionReason;

    public void setRetentionReason(final RetentionReason reason) {
        this.retentionReason = reason;
        reason.setReferralResolution(this);
    }

    public boolean isType(final ResolutionType type) {
        return type == resolutionType;
    }
}
