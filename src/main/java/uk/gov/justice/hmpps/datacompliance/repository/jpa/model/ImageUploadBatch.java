package uk.gov.justice.hmpps.datacompliance.repository.jpa.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"batchId"})
@Table(name = "IMAGE_UPLOAD_BATCH")
public class ImageUploadBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BATCH_ID", nullable = false)
    private Long batchId;

    @NotNull
    @Column(name = "UPLOAD_START_DATE_TIME", nullable = false)
    private LocalDateTime uploadStartDateTime;

    @Column(name = "UPLOAD_END_DATE_TIME")
    private LocalDateTime uploadEndDateTime;

    @Column(name = "UPLOAD_COUNT")
    private Long uploadCount;
}
