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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"dataDuplicateId"})
@Table(name = "DATA_DUPLICATE")
public class DataDuplicate {

    public enum Method {
        ID,
        DATABASE,
        ANALYTICAL_PLATFORM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DATA_DUPLICATE_ID", nullable = false)
    private Long dataDuplicateId;

    @NotNull
    @Length(max = 10)
    @Column(name = "REFERENCE_OFFENDER_NO", nullable = false)
    private String referenceOffenderNo;

    @NotNull
    @Length(max = 10)
    @Column(name = "DUPLICATE_OFFENDER_NO", nullable = false)
    private String duplicateOffenderNo;

    @NotNull
    @Column(name = "DETECTION_DATE_TIME", nullable = false)
    private LocalDateTime detectionDateTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "METHOD", nullable = false)
    private Method method;

    @Column(name = "CONFIDENCE", precision = 4, scale = 2)
    private Double confidence;
}
