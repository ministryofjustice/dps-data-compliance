package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
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
@EqualsAndHashCode(of = {"resolutionId"})
@ToString(exclude = "offenderDeletionReferral") // to avoid circular reference
@Table(name = "REFERRAL_RESOLUTION")
public class ReferralResolution implements OffenderEntity {

    public enum ResolutionStatus {
        PENDING,
        RETAINED,
        DELETION_GRANTED,
        DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESOLUTION_ID", nullable = false)
    private Long resolutionId;

    @NotNull
    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "REFERRAL_ID", nullable = false)
    private OffenderDeletionReferral offenderDeletionReferral;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "RESOLUTION_STATUS", nullable = false)
    private ReferralResolution.ResolutionStatus resolutionStatus;

    @NotNull
    @Column(name = "RESOLUTION_DATE_TIME")
    private LocalDateTime resolutionDateTime;

    @OneToMany(mappedBy = "referralResolution", cascade = PERSIST, fetch = LAZY)
    private final List<RetentionCheck> retentionChecks = new ArrayList<>();

    public ReferralResolution addRetentionCheck(final RetentionCheck reason) {
        this.retentionChecks.add(reason);
        reason.setReferralResolution(this);
        return this;
    }

    public boolean isType(final ResolutionStatus type) {
        return type == resolutionStatus;
    }

    @Override
    public OffenderNumber getOffenderNumber() {
        return getOffenderDeletionReferral().getOffenderNumber();
    }
}
