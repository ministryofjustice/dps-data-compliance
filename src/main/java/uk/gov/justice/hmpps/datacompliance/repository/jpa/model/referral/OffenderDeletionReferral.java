package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral;

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
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"referralId"})
@Table(name = "OFFENDER_DELETION_REFERRAL")
public class OffenderDeletionReferral {

    // Not using @AllArgsConstructor so that retentionResolution
    // does not appear in the builder:
    @Builder
    public OffenderDeletionReferral(final Long referralId,
                                    final OffenderDeletionBatch offenderDeletionBatch,
                                    final String offenderNo,
                                    final String firstName,
                                    final String middleName,
                                    final String lastName,
                                    final LocalDate birthDate,
                                    final LocalDateTime receivedDateTime) {
        this.referralId = referralId;
        this.offenderDeletionBatch = offenderDeletionBatch;
        this.offenderNo = offenderNo;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.receivedDateTime = receivedDateTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFERRAL_ID", nullable = false)
    private Long referralId;

    @NotNull
    @OneToOne
    @JoinColumn(name = "BATCH_ID", nullable = false)
    private OffenderDeletionBatch offenderDeletionBatch;

    @NotNull
    @Length(max = 10)
    @Column(name = "OFFENDER_NO", nullable = false)
    private String offenderNo;

    @Length(max = 35)
    @Column(name = "FIRST_NAME")
    private String firstName;

    @Length(max = 35)
    @Column(name = "MIDDLE_NAME")
    private String middleName;

    @NotNull
    @Length(max = 35)
    @Column(name = "LAST_NAME", nullable = false)
    private String lastName;

    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @NotNull
    @Column(name = "RECEIVED_DATE_TIME", nullable = false)
    private LocalDateTime receivedDateTime;

    @OneToMany(mappedBy = "offenderDeletionReferral", cascade = PERSIST, fetch = LAZY)
    private final List<ReferredOffenderBooking> offenderBookings = new ArrayList<>();

    @OneToOne(mappedBy = "offenderDeletionReferral", cascade = PERSIST, fetch = LAZY)
    private ReferralResolution referralResolution;

    public void addReferredOffenderBooking(final ReferredOffenderBooking booking) {
        this.offenderBookings.add(booking);
        booking.setOffenderDeletionReferral(this);
    }

    public Optional<ReferralResolution> getReferralResolution() {
        return Optional.ofNullable(referralResolution);
    }

    public void setReferralResolution(final ReferralResolution resolution) {
        this.referralResolution = resolution;
        resolution.setOffenderDeletionReferral(this);
    }
}
