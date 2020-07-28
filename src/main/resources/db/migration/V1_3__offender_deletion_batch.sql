DROP TABLE IF EXISTS OFFENDER_DELETION_BATCH;

CREATE TABLE OFFENDER_DELETION_BATCH
(
  BATCH_ID                        BIGSERIAL       NOT NULL,
  REQUEST_DATE_TIME               TIMESTAMP       NOT NULL,
  REFERRAL_COMPLETION_DATE_TIME   TIMESTAMP,
  WINDOW_START_DATE_TIME          TIMESTAMP,
  WINDOW_END_DATE_TIME            TIMESTAMP,
  REMAINING_IN_WINDOW             INTEGER,
  BATCH_TYPE                      VARCHAR(255)    NOT NULL,
  COMMENT_TEXT                    VARCHAR(4000),

  CONSTRAINT OFF_DEL_BAT_PK       PRIMARY KEY (BATCH_ID)
);

COMMENT ON TABLE OFFENDER_DELETION_BATCH IS 'Tracks batched requests to delete offenders';

COMMENT ON COLUMN OFFENDER_DELETION_BATCH.BATCH_ID IS 'Primary key id';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.REQUEST_DATE_TIME IS 'The timestamp of the deletion request';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.REFERRAL_COMPLETION_DATE_TIME IS 'Confirmation timestamp that all pending deletions have been added to the queue for this batch';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.WINDOW_START_DATE_TIME IS 'Batches of deletions are conducted by selecting offenders that were due for deletion between two timestamps. This is the timestamp marking the start of this window.';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.WINDOW_END_DATE_TIME IS 'Batches of deletions are conducted by selecting offenders that were due for deletion between two timestamps. This is the timestamp marking the end of this window.';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.REMAINING_IN_WINDOW IS 'The number of offenders eligible for referral within the window that have not yet been referred';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.BATCH_TYPE IS 'The type of the offender deletion (e.g. SCHEDULED or AD_HOC)';
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.COMMENT_TEXT IS 'An explanation of why the deletion was required';

CREATE INDEX OFF_DEL_BAT_RDT_IDX ON OFFENDER_DELETION_BATCH(REQUEST_DATE_TIME);
CREATE INDEX OFF_DEL_BAT_WSDT_IDX ON OFFENDER_DELETION_BATCH(WINDOW_START_DATE_TIME);
CREATE INDEX OFF_DEL_BAT_WEDT_IDX ON OFFENDER_DELETION_BATCH(WINDOW_END_DATE_TIME);
CREATE INDEX OFF_DEL_BAT_BT_IDX ON OFFENDER_DELETION_BATCH(BATCH_TYPE);
