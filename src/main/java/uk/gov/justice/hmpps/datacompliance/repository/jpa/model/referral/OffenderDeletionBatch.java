package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@EqualsAndHashCode(of = {"batchId"})
@Table(name = "OFFENDER_DELETION_BATCH")
public class OffenderDeletionBatch {

    public enum BatchType {
        SCHEDULED,
        AD_HOC
    }

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

    @Column(name = "WINDOW_START_DATE_TIME")
    private LocalDateTime windowStartDateTime;

    @Column(name = "WINDOW_END_DATE_TIME")
    private LocalDateTime windowEndDateTime;

    @Column(name = "REMAINING_IN_WINDOW")
    private Integer remainingInWindow;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "BATCH_TYPE", nullable = false)
    private BatchType batchType;

    @Column(name = "COMMENT_TEXT")
    private String commentText;

    public boolean hasRemainingInWindow() {
        return remainingInWindow != null && remainingInWindow != 0;
    }
}
