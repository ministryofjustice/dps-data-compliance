package uk.gov.justice.hmpps.datacompliance.web.dto;

import io.swagger.annotations.ApiModel;

@ApiModel
public enum ManualRetentionReasonCode {
    CHILD_SEX_ABUSE,
    HIGH_PROFILE,
    LITIGATION_DISPUTE,
    LOOKED_AFTER_CHILDREN,
    MAPPA,
    FOI_SAR,
    UAL,
    OTHER
}
