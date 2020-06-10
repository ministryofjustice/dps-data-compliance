package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;

import javax.persistence.Column;
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

import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.LAZY;

@Entity
public abstract class RetentionCheckDataDuplicate extends RetentionCheck {

    @OneToMany(mappedBy = "retentionCheck", cascade = {PERSIST, MERGE}, fetch = LAZY)
    private final List<RetentionReasonDataDuplicate> dataDuplicates = new ArrayList<>();

    RetentionCheckDataDuplicate() {
        this(null, null);
    }

    public RetentionCheckDataDuplicate(final String checkType, final Status status) {
        super(null, null, checkType, status);
    }

    public RetentionCheckDataDuplicate addDataDuplicates(final List<DataDuplicate> dataDuplicates) {
        dataDuplicates.forEach(this::addDataDuplicate);
        return this;
    }

    public List<DataDuplicate> getDataDuplicates() {
        return dataDuplicates.stream()
                .map(RetentionReasonDataDuplicate::getDataDuplicate)
                .collect(toList());
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
