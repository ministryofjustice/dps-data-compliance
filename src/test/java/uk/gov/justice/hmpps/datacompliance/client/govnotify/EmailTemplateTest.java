package uk.gov.justice.hmpps.datacompliance.client.govnotify;

import org.junit.jupiter.api.Test;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


class EmailTemplateTest {

    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final String OFFENDER_FIRST_NAME = "TOM";
    private static final String OFFENDER_MIDDLE_NAME = "TONY";
    private static final String OFFENDER_LAST_NAME = "SMITH";
    private static final String AGENCY_LOCATION_ID = "LEI";
    private static final long REFERRAL_ID = 123L;
    private static final String INTENTION_TO_DELETE_TEMPLATE_ID = "intentionToDeleteTemplateId";
    private static final String DIGITAL_SERVICE_URL = "https://some-domain.justice.gov.uk";
    private static final String EMAIL = "someEmail@omu.gov.uk";
    private static final Duration DURATION = Duration.ofDays(1);


    @Test
    public void ofIntentionToDelete() {

        final var referral = buildReferral();

        final var emailTemplate = EmailTemplate.ofIntentionToDelete(referral, EMAIL, DIGITAL_SERVICE_URL, INTENTION_TO_DELETE_TEMPLATE_ID, DURATION);
        final var emailParameters = emailTemplate.getEmailParameters();


        assertThat(emailTemplate.getReference()).isEqualTo("Ref-A1234AA-LEI");

        assertThat(emailParameters).extracting(EmailParameters::getNomisNumber).isEqualTo(OFFENDER_NUMBER);
        assertThat(emailParameters).extracting(EmailParameters::getOffenderName).isEqualTo(OFFENDER_FIRST_NAME + " " + OFFENDER_MIDDLE_NAME + " " + OFFENDER_LAST_NAME);
        assertThat(emailParameters).extracting(EmailParameters::getDpsRetentionUrl).asString().contains("/offenders/" + OFFENDER_NUMBER + "/retention-reasons");
        assertThat(emailParameters).extracting(EmailParameters::getOmu).isEqualTo(AGENCY_LOCATION_ID);
        assertThat(emailParameters).extracting(EmailParameters::getReviewPeriodDuration).isEqualTo("1 days");

        final var emailParamMap = emailParameters.toMap();

        assertThat(emailParamMap.get("omu")).asString().isEqualTo(AGENCY_LOCATION_ID);
        assertThat(emailParamMap.get("offender_name")).asString().isEqualTo(OFFENDER_FIRST_NAME + " " + OFFENDER_MIDDLE_NAME + " " + OFFENDER_LAST_NAME);
        assertThat(emailParamMap.get("nomis_number")).asString().isEqualTo(OFFENDER_NUMBER);
        assertThat(emailParamMap.get("dps_retention_url")).asString().contains("/offenders/" + OFFENDER_NUMBER + "/retention-reasons");
        assertThat(emailParamMap.get("review_period_duration")).asString().contains("1 days");
    }


    private OffenderDeletionReferral buildReferral() {
        return OffenderDeletionReferral.builder()
            .referralId(REFERRAL_ID)
            .offenderNo(OFFENDER_NUMBER)
            .agencyLocationId(AGENCY_LOCATION_ID)
            .firstName(OFFENDER_FIRST_NAME)
            .middleName(OFFENDER_MIDDLE_NAME)
            .lastName(OFFENDER_LAST_NAME)
            .build();
    }

}