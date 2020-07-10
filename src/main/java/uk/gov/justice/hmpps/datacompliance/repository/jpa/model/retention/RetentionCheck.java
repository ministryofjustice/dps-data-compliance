package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import static javax.persistence.FetchType.EAGER;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;

@Data
@Entity
@Inheritance
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorColumn(name = "CHECK_TYPE")
@ToString(exclude = {"referralResolution"}) // to avoid circular reference
@EqualsAndHashCode(of = {"retentionCheckId"})
@Table(name = "RETENTION_CHECK")
public abstract class RetentionCheck implements OffenderEntity {

    public enum Status {
        PENDING,
        RETENTION_REQUIRED,
        RETENTION_NOT_REQUIRED,
        FALSE_POSITIVE,
        DISABLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RETENTION_CHECK_ID", nullable = false)
    private Long retentionCheckId;

    @NotNull
    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "RESOLUTION_ID", nullable = false)
    private ReferralResolution referralResolution;

    @NotNull
    @Column(name = "CHECK_TYPE", nullable = false, insertable = false, updatable = false)
    private String checkType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "CHECK_STATUS", nullable = false)
    private RetentionCheck.Status checkStatus;

    @Override
    public OffenderNumber getOffenderNumber() {
        return getReferralResolution().getOffenderNumber();
    }

    public boolean isPending() {
        return checkStatus == PENDING;
    }

    public boolean isStatus(final Status value) {
        return checkStatus == value;
    }

    public boolean isType(final String value) {
        return value.equalsIgnoreCase(checkType);
    }
}
