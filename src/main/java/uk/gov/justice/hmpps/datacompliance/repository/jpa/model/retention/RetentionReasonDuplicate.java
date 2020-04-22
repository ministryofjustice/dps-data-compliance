package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue(RetentionReasonDuplicate.DUPLICATE)
public class RetentionReasonDuplicate extends RetentionReason {

    public static final String DUPLICATE = "DUPLICATE";

    @OneToMany(mappedBy = "retentionReason", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private final List<RetentionReasonDataDuplicate> dataDuplicates = new ArrayList<>();

    @OneToMany(mappedBy = "retentionReason", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private final List<RetentionReasonImageDuplicate> imageDuplicates = new ArrayList<>();

    public RetentionReasonDuplicate() {
        super(null, null, DUPLICATE);
    }

    public RetentionReasonDuplicate addDataDuplicate(final DataDuplicate dataDuplicate) {
        dataDuplicates.add(RetentionReasonDataDuplicate.builder()
                .retentionReason(this)
                .dataDuplicate(dataDuplicate)
                .build());
        return this;
    }

    public RetentionReasonDuplicate addImageDuplicate(final ImageDuplicate imageDuplicate) {
        imageDuplicates.add(RetentionReasonImageDuplicate.builder()
                .retentionReason(this)
                .imageDuplicate(imageDuplicate)
                .build());
        return this;
    }

    @Data
    @Entity
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"duplicateId"})
    @Table(name = "RETENTION_REASON_DATA_DUPLICATE")
    private static class RetentionReasonDataDuplicate {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "RET_REASON_DATA_DUP_ID", nullable = false)
        private Long duplicateId;

        @ManyToOne
        @JoinColumn(name = "RETENTION_REASON_ID")
        private RetentionReasonDuplicate retentionReason;

        @ManyToOne
        @JoinColumn(name = "DATA_DUPLICATE_ID")
        private DataDuplicate dataDuplicate;
    }

    @Data
    @Entity
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"duplicateId"})
    @Table(name = "RETENTION_REASON_IMAGE_DUPLICATE")
    private static class RetentionReasonImageDuplicate {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "RET_REASON_IMG_DUP_ID", nullable = false)
        private Long duplicateId;

        @ManyToOne
        @JoinColumn(name = "RETENTION_REASON_ID")
        private RetentionReasonDuplicate retentionReason;

        @ManyToOne
        @JoinColumn(name = "IMAGE_DUPLICATE_ID")
        private ImageDuplicate imageDuplicate;
    }
}
