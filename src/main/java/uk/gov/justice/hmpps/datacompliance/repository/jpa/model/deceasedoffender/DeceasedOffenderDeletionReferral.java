package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender;

import lombok.*;
import org.hibernate.validator.constraints.Length;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderEntity;

import javax.persistence.*;
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
@Table(name = "DECEASED_OFFENDER_DELETION_REFERRAL")
public class DeceasedOffenderDeletionReferral implements OffenderEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFERRAL_ID", nullable = false)
    private Long referralId;

    @NotNull
    @OneToOne
    @JoinColumn(name = "BATCH_ID", nullable = false)
    private DeceasedOffenderDeletionBatch deceasedOffenderDeletionBatch;

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
    @Column(name = "DECEASED_DATE", nullable = false)
    private LocalDate deceasedDate;

    @NotNull
    @Column(name = "DELETION_DATE_TIME", nullable = false)
    private LocalDateTime deletionDateTime;

    @OneToMany(mappedBy = "deceasedOffenderDeletionReferral", cascade = ALL, fetch = LAZY)
    private final List<ReferredDeceasedOffenderAlias> offenderAliases = new ArrayList<>();

    public void addReferredOffenderAlias(final ReferredDeceasedOffenderAlias alias) {
        this.offenderAliases.add(alias);
        alias.setDeceasedOffenderDeletionReferral(this);
    }

    @Override
    public OffenderNumber getOffenderNumber() {
        return new OffenderNumber(offenderNo);
    }


}
