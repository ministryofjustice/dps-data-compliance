UPDATE RETENTION_REASON_CODE SET DISPLAY_ORDER = 8 WHERE RETENTION_REASON_CODE_ID = 'OTHER';

INSERT INTO RETENTION_REASON_CODE(RETENTION_REASON_CODE_ID, DISPLAY_NAME, ALLOW_REASON_DETAILS, DISPLAY_ORDER) VALUES ('RESTRICTED_PATIENT', 'Restricted patient with invalid state in NOMIS', FALSE, 7);
