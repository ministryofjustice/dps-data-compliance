package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender;

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
@ToString(exclude = "deceasedOffenderDeletionReferral") // to avoid circular reference
@Table(name = "REFERRED_DECEASED_OFFENDER_ALIAS")
public class ReferredDeceasedOffenderAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OFFENDER_ALIAS_ID", nullable = false)
    private Long aliasId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "REFERRAL_ID")
    private DeceasedOffenderDeletionReferral deceasedOffenderDeletionReferral;

    @NotNull
    @Column(name = "OFFENDER_ID", nullable = false)
    private Long offenderId;

    @Column(name = "OFFENDER_BOOK_ID")
    private Long offenderBookId;
}
