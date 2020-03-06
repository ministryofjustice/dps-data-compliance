package uk.gov.justice.hmpps.datacompliance.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
@EqualsAndHashCode(of = {"referredOffenderBookingId"})
@Table(name = "REFERRED_OFFENDER_BOOKING")
public class ReferredOffenderBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFERRED_OFFENDER_BOOKING_ID", nullable = false)
    private Long referredOffenderBookingId;

    @ManyToOne
    @JoinColumn(name = "REFERRAL_ID")
    private OffenderDeletionReferral offenderDeletionReferral;

    @NotNull
    @Column(name = "OFFENDER_ID", nullable = false)
    private Long offenderId;

    @NotNull
    @Column(name = "OFFENDER_BOOK_ID", nullable = false)
    private Long offenderBookId;
}
