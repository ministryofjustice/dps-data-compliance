package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.LAZY;

@Entity
@DiscriminatorValue(RetentionCheckDataDuplicate.DUPLICATE)
public class RetentionCheckDataDuplicate extends RetentionCheck {

    public static final String DUPLICATE = "DATA_DUPLICATE";

    @OneToMany(mappedBy = "retentionCheck", cascade = PERSIST, fetch = LAZY)
    private final List<RetentionReasonDataDuplicate> dataDuplicates = new ArrayList<>();

    public RetentionCheckDataDuplicate() {
        this(null);
    }

    public RetentionCheckDataDuplicate(final Status status) {
        super(null, null, DUPLICATE, status);
    }

    public RetentionCheckDataDuplicate addDataDuplicates(final List<DataDuplicate> dataDuplicates) {
        dataDuplicates.forEach(this::addDataDuplicate);
        return this;
    }

    private void addDataDuplicate(final DataDuplicate dataDuplicate) {
        dataDuplicates.add(RetentionReasonDataDuplicate.builder()
                .retentionCheck(this)
                .dataDuplicate(dataDuplicate)
                .build());
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

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "RETENTION_CHECK_ID")
        private RetentionCheckDataDuplicate retentionCheck;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "DATA_DUPLICATE_ID")
        private DataDuplicate dataDuplicate;
    }
}
