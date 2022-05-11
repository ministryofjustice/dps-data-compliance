package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static javax.persistence.FetchType.LAZY;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload.ImageUploadStatus.SUCCESS;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"uploadId"})
@Table(name = "OFFENDER_IMAGE_UPLOAD")
public class OffenderImageUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UPLOAD_ID", nullable = false)
    private Long uploadId;
    @NotNull
    @Length(max = 10)
    @Column(name = "OFFENDER_NO", nullable = false)
    private String offenderNo;
    @NotNull
    @Column(name = "OFFENDER_IMAGE_ID", nullable = false)
    private Long imageId;
    @Length(max = 255)
    @Column(name = "FACE_ID")
    private String faceId;
    @NotNull
    @Column(name = "UPLOAD_DATE_TIME", nullable = false)
    private LocalDateTime uploadDateTime;
    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "BATCH_ID", nullable = false)
    private ImageUploadBatch imageUploadBatch;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "UPLOAD_STATUS", nullable = false)
    private ImageUploadStatus uploadStatus;
    @Length(max = 255)
    @Column(name = "UPLOAD_ERROR_REASON")
    private String uploadErrorReason;

    public boolean isSuccess() {
        return SUCCESS == uploadStatus;
    }

    public enum ImageUploadStatus {
        SUCCESS,
        ERROR,
        DELETED
    }
}
