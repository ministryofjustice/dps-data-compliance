package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.LAZY;

@Entity
@DiscriminatorValue(RetentionCheckManual.MANUAL_RETENTION)
public class RetentionCheckManual extends RetentionCheck {

    public static final String MANUAL_RETENTION = "MANUAL_RETENTION";

    @OneToOne(mappedBy = "retentionCheck", cascade = PERSIST, fetch = LAZY)
    private RetentionReasonManual retentionReasonManual;

    private RetentionCheckManual() {
        this(null);
    }

    public RetentionCheckManual(final Status status) {
        super(null, null, MANUAL_RETENTION, status);
    }

    public RetentionCheckManual setManualRetention(final ManualRetention manualRetention) {
        this.retentionReasonManual = RetentionReasonManual.builder()
                .retentionCheck(this)
                .manualRetention(manualRetention)
                .build();
        return this;
    }

    public ManualRetention getManualRetention() {
        return retentionReasonManual.getManualRetention();
    }

    @Data
    @Entity
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"retentionReasonManualId"})
    @Table(name = "RETENTION_REASON_MANUAL")
    private static class RetentionReasonManual {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "RETENTION_REASON_MANUAL_ID", nullable = false)
        private Long retentionReasonManualId;

        @OneToOne(fetch = LAZY)
        @JoinColumn(name = "RETENTION_CHECK_ID")
        private RetentionCheckManual retentionCheck;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "MANUAL_RETENTION_ID")
        private ManualRetention manualRetention;

    }
}
