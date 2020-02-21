package uk.gov.justice.hmpps.datacompliance.model;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "BATCH_ID")
    private ImageUploadBatch imageUploadBatch;

    @Length(max = 255)
    @Column(name = "UPLOAD_ERROR_REASON")
    private String uploadErrorReason;
}
