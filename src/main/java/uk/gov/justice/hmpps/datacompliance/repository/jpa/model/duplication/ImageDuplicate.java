package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication;

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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"imageDuplicateId"})
@Table(name = "IMAGE_DUPLICATE")
public class ImageDuplicate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IMAGE_DUPLICATE_ID", nullable = false)
    private Long imageDuplicateId;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "REFERENCE_OFFENDER_UPLOAD_ID", referencedColumnName = "UPLOAD_ID", nullable = false)
    private OffenderImageUpload referenceOffenderImageUpload;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "DUPLICATE_OFFENDER_UPLOAD_ID", referencedColumnName = "UPLOAD_ID", nullable = false)
    private OffenderImageUpload duplicateOffenderImageUpload;

    @NotNull
    @Column(name = "DETECTION_DATE_TIME", nullable = false)
    private LocalDateTime detectionDateTime;
}
