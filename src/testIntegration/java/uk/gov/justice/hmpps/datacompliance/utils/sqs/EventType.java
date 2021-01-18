package uk.gov.justice.hmpps.datacompliance.utils.sqs;

public class EventType {

    public static class Response {
        public static final String ADHOC_OFFENDER_DELETION_EVENT = "DATA_COMPLIANCE_AD-HOC-OFFENDER-DELETION";
        public static final String OFFENDER_PENDING_DELETION_EVENT = "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION";
        public static final String OFFENDER_PENDING_DELETION_REFERRAL_COMPLETE_EVENT = "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-REFERRAL-COMPLETE";
        public static final String OFFENDER_DELETION_COMPLETE_EVENT = "DATA_COMPLIANCE_OFFENDER-DELETION-COMPLETE";
        public static final String DATA_DUPLICATE_ID_RESULT = "DATA_COMPLIANCE_DATA-DUPLICATE-ID-RESULT";
        public static final String DATA_DUPLICATE_DB_RESULT = "DATA_COMPLIANCE_DATA-DUPLICATE-DB-RESULT";
        public static final String FREE_TEXT_MORATORIUM_RESULT = "DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-RESULT";
    }
    
    public static class Request{
        public static final String REFERRAL_REQUEST = "DATA_COMPLIANCE_REFERRAL-REQUEST";
        public static final String AD_HOC_REFERRAL_REQUEST = "DATA_COMPLIANCE_AD-HOC-REFERRAL-REQUEST";
        public static final String OFFENDER_DELETION_GRANTED = "DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED";
        public static final String DATA_DUPLICATE_ID_CHECK = "DATA_COMPLIANCE_DATA-DUPLICATE-ID-CHECK";
        public static final String DATA_DUPLICATE_DB_CHECK = "DATA_COMPLIANCE_DATA-DUPLICATE-DB-CHECK";
        public static final String FREE_TEXT_MORATORIUM_CHECK = "DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-CHECK";
    }
    
    
    
}
