ALTER TABLE OFFENDER_DELETION_BATCH ADD REMAINING_IN_WINDOW INTEGER;
COMMENT ON COLUMN OFFENDER_DELETION_BATCH.REMAINING_IN_WINDOW IS 'The number of offenders eligible for referral within the window that have not yet been referred';
