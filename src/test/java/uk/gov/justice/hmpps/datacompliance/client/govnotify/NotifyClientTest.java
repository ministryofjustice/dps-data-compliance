package uk.gov.justice.hmpps.datacompliance.client.govnotify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.config.OffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.time.Duration;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyClientTest {

    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final String OFFENDER_FIRST_NAME = "TOM";
    private static final String OFFENDER_MIDDLE_NAME = "TONY";
    private static final String OFFENDER_LAST_NAME = "SMITH";
    private static final String AGENCY_LOCATION_ID = "LEI";
    private static final long REFERRAL_ID = 123L;
    private static final String EMAIL = "someEmail@omu.gov.uk";
    private static final String DIGITAL_SERVICE_URL = "https://some-domain.justice.gov.uk";
    private static final String INTENTION_TO_DELETE_TEMPLATE_ID = "intentionToDeleteTemplateId";
    private static final String OFFENDER_DATA_CLEANSED_TEMPLATE_ID = "offenderDataCleansedTemplateId";
    private static final Duration DURATION = Duration.ofDays(1);

    @Mock
    private NotificationClient notificationClient;

    private NotifyClient notifyClient;

    @BeforeEach
    public void setUp() {
        notifyClient = new NotifyClient(true, INTENTION_TO_DELETE_TEMPLATE_ID, OFFENDER_DATA_CLEANSED_TEMPLATE_ID, DIGITAL_SERVICE_URL, DURATION, notificationClient);
    }

    @Test
    void sendIntentionToDeleteEmail() throws NotificationClientException {

        notifyClient.sendIntentionToDeleteEmail(buildReferral(), EMAIL);

        verify(notificationClient).sendEmail(INTENTION_TO_DELETE_TEMPLATE_ID, EMAIL, intentionToDeleteParams(), "Ref-A1234AA-LEI");
    }

    @Test
    void sendOffenderDataCleansedEmail() throws NotificationClientException {

        notifyClient.sendOffenderDataCleansedEmail(buildReferral(), EMAIL);

        verify(notificationClient).sendEmail(OFFENDER_DATA_CLEANSED_TEMPLATE_ID, EMAIL, offenderDataCleansedParams(), "Ref-A1234AA-LEI");
    }

    @Test
    public void sendEmail() throws NotificationClientException {

        final EmailTemplate emailTemplate = EmailTemplate.ofIntentionToDelete(buildReferral(), EMAIL, DIGITAL_SERVICE_URL, INTENTION_TO_DELETE_TEMPLATE_ID, DURATION);

        notifyClient.sendEmail(emailTemplate);

        verify(notificationClient).sendEmail(INTENTION_TO_DELETE_TEMPLATE_ID, EMAIL, intentionToDeleteParams(), "Ref-A1234AA-LEI");
    }

    @Test
    public void sendEmailSwallowsClientError() throws NotificationClientException {

        final EmailTemplate emailTemplate = EmailTemplate.ofIntentionToDelete(buildReferral(), EMAIL, DIGITAL_SERVICE_URL, INTENTION_TO_DELETE_TEMPLATE_ID, DURATION);

        when(notificationClient.sendEmail(INTENTION_TO_DELETE_TEMPLATE_ID, EMAIL, intentionToDeleteParams(), "Ref-A1234AA-LEI"))
            .thenThrow(new NotificationClientException("\t[{\n" +
                "\"error\": \"BadRequestError\",\n" +
                "\"message\": \"Can't send to this recipient using a team-only API key\"\n" + "}]"));

        notifyClient.sendEmail(emailTemplate);
    }

    @Test
    public void sendEmailSwallowsUnexpectedException() throws NotificationClientException {

        final EmailTemplate emailTemplate = EmailTemplate.ofIntentionToDelete(buildReferral(), EMAIL, DIGITAL_SERVICE_URL, INTENTION_TO_DELETE_TEMPLATE_ID, DURATION);

        when(notificationClient.sendEmail(INTENTION_TO_DELETE_TEMPLATE_ID, EMAIL, intentionToDeleteParams(), "Ref-A1234AA-LEI"))
            .thenThrow(new RuntimeException());

        notifyClient.sendEmail(emailTemplate);
    }


    private Map<String, String> intentionToDeleteParams() {
        return Map.of(
            "nomis_number", OFFENDER_NUMBER,
            "offender_name", OFFENDER_FIRST_NAME + " " + OFFENDER_MIDDLE_NAME + " " + OFFENDER_LAST_NAME,
            "dps_retention_url", DIGITAL_SERVICE_URL + "/offenders/" + OFFENDER_NUMBER + "/retention-reasons",
            "omu", AGENCY_LOCATION_ID,
            "review_period_duration", "1 days");
    }

    private Map<String, String> offenderDataCleansedParams() {
        return Map.of(
            "nomis_number", OFFENDER_NUMBER,
            "offender_name", OFFENDER_FIRST_NAME + " " + OFFENDER_MIDDLE_NAME + " " + OFFENDER_LAST_NAME,
            "omu", AGENCY_LOCATION_ID);
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