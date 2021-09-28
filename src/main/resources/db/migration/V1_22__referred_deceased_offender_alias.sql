DROP TABLE IF EXISTS REFERRED_DECEASED_OFFENDER_ALIAS;

CREATE TABLE REFERRED_DECEASED_OFFENDER_ALIAS
(
  OFFENDER_ALIAS_ID                   BIGSERIAL       NOT NULL,
  REFERRAL_ID                         BIGINT          NOT NULL,
  OFFENDER_ID                         NUMERIC(10, 0)  NOT NULL,
  OFFENDER_BOOK_ID                    NUMERIC(10, 0),

  CONSTRAINT REF_DEC_OFF_ALI_PK          PRIMARY KEY (OFFENDER_ALIAS_ID),
  CONSTRAINT REF_DEC_OFF_ALI_REFERRAL_FK FOREIGN KEY (REFERRAL_ID) REFERENCES DECEASED_OFFENDER_DELETION_REFERRAL(REFERRAL_ID)
);

COMMENT ON TABLE REFERRED_DECEASED_OFFENDER_ALIAS IS 'Table to store booking ids for a referred offender';

COMMENT ON COLUMN REFERRED_DECEASED_OFFENDER_ALIAS.OFFENDER_ALIAS_ID IS 'Primary key id';
COMMENT ON COLUMN REFERRED_DECEASED_OFFENDER_ALIAS.OFFENDER_ID IS 'This is the primary key in NOMIS for the OFFENDERS table';
COMMENT ON COLUMN REFERRED_DECEASED_OFFENDER_ALIAS.OFFENDER_BOOK_ID IS 'This is the primary key in NOMIS for the OFFENDER_BOOKINGS table';
COMMENT ON COLUMN REFERRED_DECEASED_OFFENDER_ALIAS.REFERRAL_ID IS 'This links the ids here to the referred offender';

CREATE INDEX REF_DEC_OFF_ALI_RI_IDX ON REFERRED_DECEASED_OFFENDER_ALIAS(REFERRAL_ID);
