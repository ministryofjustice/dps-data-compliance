package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral;

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
@EqualsAndHashCode(of = {"referredOffenderAliasId"})
@ToString(exclude = "offenderDeletionReferral") // to avoid circular reference
@Table(name = "REFERRED_OFFENDER_ALIAS")
public class ReferredOffenderAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFERRED_OFFENDER_ALIAS_ID", nullable = false)
    private Long referredOffenderAliasId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "REFERRAL_ID")
    private OffenderDeletionReferral offenderDeletionReferral;

    @NotNull
    @Column(name = "OFFENDER_ID", nullable = false)
    private Long offenderId;

    @Column(name = "OFFENDER_BOOK_ID")
    private Long offenderBookId;
}
