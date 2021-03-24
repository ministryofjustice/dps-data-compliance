DROP TABLE IF EXISTS OFFENDER_UAL;

CREATE TABLE OFFENDER_UAL
(
  OFFENDER_UAL_ID                  BIGSERIAL       NOT NULL,
  OFFENDER_NO                      VARCHAR(35),
  OFFENDER_BOOKING_NO              VARCHAR(35),
  OFFENDER_CRO_PNC                 VARCHAR(35),
  FIRST_NAMES                      VARCHAR(50),
  LAST_NAME                        VARCHAR(50),
  OFFENCE_DESCRIPTION              VARCHAR(1000),
  UPLOAD_DATE_TIME                 TIMESTAMP       NOT NULL,
  USER_ID                          VARCHAR(32)     NOT NULL,

  CONSTRAINT OFFENDER_UAL_PK PRIMARY KEY (OFFENDER_UAL_ID)
);

COMMENT ON TABLE OFFENDER_UAL IS 'Records unlawfully at large offender data';

COMMENT ON COLUMN OFFENDER_UAL.OFFENDER_UAL_ID IS 'Primary key id';
COMMENT ON COLUMN OFFENDER_UAL.OFFENDER_NO IS 'The NOMS offender number';
COMMENT ON COLUMN OFFENDER_UAL.OFFENDER_BOOKING_NO IS 'This is known as the "prison number" or "offender booking number" in Nomis';
COMMENT ON COLUMN OFFENDER_UAL.OFFENDER_CRO_PNC IS 'This may contain offender CRO or PNC or both';
COMMENT ON COLUMN OFFENDER_UAL.FIRST_NAMES IS 'The offenders first names';
COMMENT ON COLUMN OFFENDER_UAL.LAST_NAME IS 'The offenders last name';
COMMENT ON COLUMN OFFENDER_UAL.OFFENCE_DESCRIPTION IS 'The description of the offence stated in the UAL report';
COMMENT ON COLUMN OFFENDER_UAL.UPLOAD_DATE_TIME IS 'The timestamp of this creation/update';
COMMENT ON COLUMN OFFENDER_UAL.USER_ID IS 'ID of the user who created the record';


CREATE INDEX OFF_UAL_OFF_NO_IDX ON OFFENDER_UAL(OFFENDER_NO);
