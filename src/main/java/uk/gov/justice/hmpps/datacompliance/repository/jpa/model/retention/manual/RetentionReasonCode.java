package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual;

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
import javax.validation.constraints.NotNull;

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
        UAL,
        OTHER
    }

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "RETENTION_REASON_CODE_ID", nullable = false)
    private Code retentionReasonCodeId;

    @NotNull
    @Column(name = "DISPLAY_NAME", nullable = false)
    private String displayName;

    @NotNull
    @Column(name = "ALLOW_REASON_DETAILS", nullable = false)
    private Boolean allowReasonDetails;

    @NotNull
    @Column(name = "DISPLAY_ORDER", nullable = false)
    private Integer displayOrder;
}
