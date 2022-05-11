package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "OFFENDER_UAL")
public class OffenderUalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OFFENDER_UAL_ID", nullable = false)
    private Long offenderUalId;

    @Column(name = "OFFENDER_NO")
    private String offenderNo;

    @Column(name = "OFFENDER_BOOKING_NO")
    private String offenderBookingNo;

    @Column(name = "OFFENDER_PNC")
    private String offenderPnc;

    @Column(name = "OFFENDER_CRO")
    private String offenderCro;

    @Column(name = "FIRST_NAMES")
    private String firstNames;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "OFFENCE_DESCRIPTION")
    private String offenceDescription;

    @NotNull
    @Column(name = "UPLOAD_DATE_TIME", nullable = false)
    private LocalDateTime uploadDateTime;

    @NotNull
    @Column(name = "USER_ID", nullable = false)
    private String userId;
}
