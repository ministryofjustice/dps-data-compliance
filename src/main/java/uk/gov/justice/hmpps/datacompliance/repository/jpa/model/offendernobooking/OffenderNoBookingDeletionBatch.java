package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"batchId"})
@Table(name = "OFFENDER_NO_BOOKING_DELETION_BATCH")
public class OffenderNoBookingDeletionBatch {

    @Id
    @With
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BATCH_ID", nullable = false)
    private Long batchId;
    @NotNull
    @Column(name = "REQUEST_DATE_TIME", nullable = false)
    private LocalDateTime requestDateTime;
    @Column(name = "REFERRAL_COMPLETION_DATE_TIME")
    private LocalDateTime referralCompletionDateTime;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "BATCH_TYPE", nullable = false)
    private BatchType batchType;
    @Column(name = "COMMENT_TEXT")
    private String commentText;

    public enum BatchType {
        SCHEDULED,
        AD_HOC
    }


}
