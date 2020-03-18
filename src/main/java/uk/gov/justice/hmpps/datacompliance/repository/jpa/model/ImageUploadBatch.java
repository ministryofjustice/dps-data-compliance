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
import javax.persistence.Table;
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
