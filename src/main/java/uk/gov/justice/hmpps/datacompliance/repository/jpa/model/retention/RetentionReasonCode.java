package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"retentionReasonCodeId"})
@Table(name = "RETENTION_REASON_CODE")
public class RetentionReasonCode {

    public enum Code {
        CHILD_SEX_ABUSE,
        HIGH_PROFILE,
        LITIGATION_DISPUTE,
        LOOKED_AFTER_CHILDREN,
        MAPPA,
        FOI_SAR,
        OTHER
    }

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "RETENTION_REASON_CODE_ID", nullable = false)
    private Code retentionReasonCodeId;

    @Column(name = "DISPLAY_NAME", nullable = false)
    private String displayName;
}
