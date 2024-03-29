DROP TABLE IF EXISTS DECEASED_OFFENDER_DELETION_BATCH;

CREATE TABLE DECEASED_OFFENDER_DELETION_BATCH
(
  BATCH_ID                        BIGSERIAL       NOT NULL,
  REQUEST_DATE_TIME               TIMESTAMP       NOT NULL,
  REFERRAL_COMPLETION_DATE_TIME   TIMESTAMP,
  BATCH_TYPE                      VARCHAR(255)    NOT NULL,
  COMMENT_TEXT                    VARCHAR(4000),

  CONSTRAINT DEC_OFF_DEL_BAT_PK       PRIMARY KEY (BATCH_ID)
);

COMMENT ON TABLE DECEASED_OFFENDER_DELETION_BATCH IS 'Tracks batched requests to delete deceased offenders';

COMMENT ON COLUMN DECEASED_OFFENDER_DELETION_BATCH.BATCH_ID IS 'Primary key id';
COMMENT ON COLUMN DECEASED_OFFENDER_DELETION_BATCH.REQUEST_DATE_TIME IS 'The timestamp of the deceased offender deletion request';
COMMENT ON COLUMN DECEASED_OFFENDER_DELETION_BATCH.REFERRAL_COMPLETION_DATE_TIME IS 'Confirmation timestamp of the deletion of the deceased offenders data';
COMMENT ON COLUMN DECEASED_OFFENDER_DELETION_BATCH.BATCH_TYPE IS 'The type of the offender deletion (e.g. SCHEDULED or AD_HOC)';
COMMENT ON COLUMN DECEASED_OFFENDER_DELETION_BATCH.COMMENT_TEXT IS 'An explanation of why the deletion was required';

CREATE INDEX DEC_OFF_DEL_BAT_RDT_IDX ON DECEASED_OFFENDER_DELETION_BATCH(REQUEST_DATE_TIME);
CREATE INDEX DEC_OFF_DEL_BAT_BT_IDX ON DECEASED_OFFENDER_DELETION_BATCH(BATCH_TYPE);
