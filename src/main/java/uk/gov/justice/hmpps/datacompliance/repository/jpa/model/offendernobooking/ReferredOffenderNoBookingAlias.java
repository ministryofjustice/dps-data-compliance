package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"aliasId"})
@ToString(exclude = "offenderNoBookingDeletionReferral") // to avoid circular reference
@Table(name = "REFERRED_OFFENDER_NO_BOOKING_ALIAS")
public class ReferredOffenderNoBookingAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OFFENDER_ALIAS_ID", nullable = false)
    private Long aliasId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "REFERRAL_ID")
    private OffenderNoBookingDeletionReferral offenderNoBookingDeletionReferral;

    @NotNull
    @Column(name = "OFFENDER_ID", nullable = false)
    private Long offenderId;
}
