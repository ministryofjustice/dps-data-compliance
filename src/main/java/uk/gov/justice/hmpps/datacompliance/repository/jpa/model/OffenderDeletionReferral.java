package uk.gov.justice.hmpps.datacompliance.repository.jpa.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@EqualsAndHashCode(of = {"referralId"})
@Table(name = "OFFENDER_DELETION_REFERRAL")
public class OffenderDeletionReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFERRAL_ID", nullable = false)
    private Long referralId;

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

    @OneToMany(mappedBy = "offenderDeletionReferral", cascade = CascadeType.PERSIST)
    private final List<ReferredOffenderBooking> referredOffenderBookings = new ArrayList<>();

    private OffenderDeletionReferral(final Long referralId,
                                     final String offenderNo,
                                     final String firstName,
                                     final String middleName,
                                     final String lastName,
                                     final LocalDate birthDate,
                                     final LocalDateTime receivedDateTime) {
        this.referralId = referralId;
        this.offenderNo = offenderNo;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.receivedDateTime = receivedDateTime;
    }

    public void addReferredOffenderBooking(final ReferredOffenderBooking booking) {
        this.referredOffenderBookings.add(booking);
        booking.setOffenderDeletionReferral(this);
    }
}
