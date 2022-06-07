package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderEntity;

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

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"referralId"})
@Table(name = "OFFENDER_NO_BOOKING_DELETION_REFERRAL")
public class OffenderNoBookingDeletionReferral implements OffenderEntity {


    @OneToMany(mappedBy = "offenderNoBookingDeletionReferral", cascade = ALL, fetch = LAZY)
    private final List<ReferredOffenderNoBookingAlias> offenderAliases = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFERRAL_ID", nullable = false)
    private Long referralId;
    @NotNull
    @OneToOne
    @JoinColumn(name = "BATCH_ID", nullable = false)
    private OffenderNoBookingDeletionBatch offenderNoBookingDeletionBatch;
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
    @Column(name = "AGENCY_LOCATION_ID")
    private String agencyLocationId;
    @NotNull
    @Column(name = "DELETION_DATE_TIME", nullable = false)
    private LocalDateTime deletionDateTime;

    public void addReferredOffenderAlias(final ReferredOffenderNoBookingAlias alias) {
        this.offenderAliases.add(alias);
        alias.setOffenderNoBookingDeletionReferral(this);
    }

    @Override
    public OffenderNumber getOffenderNumber() {
        return new OffenderNumber(offenderNo);
    }


}
