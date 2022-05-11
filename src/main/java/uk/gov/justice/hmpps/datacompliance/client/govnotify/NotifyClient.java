package uk.gov.justice.hmpps.datacompliance.client.govnotify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.time.Duration;

import static uk.gov.justice.hmpps.datacompliance.client.govnotify.EmailTemplate.ofIntentionToDelete;
import static uk.gov.justice.hmpps.datacompliance.client.govnotify.EmailTemplate.ofOffenderDataCleansed;


@Slf4j
@Component
public class NotifyClient {

    private final boolean enabled;
    private final String intentionToDeleteTemplateId;
    private final String offenderDataCleansedTemplateId;
    private final String digitalPrisonServiceBaseUrl;
    private final Duration offenderDeletionReviewDuration;
    private final NotificationClient client;

    public NotifyClient(
        @Value("${notify.enabled}") boolean enabled,
        @Value("${notify.templates.intention-to-delete}") String intentionToDeleteTemplateId,
        @Value("${notify.templates.offender-data-cleansed}") String offenderDataCleansedTemplateId,
        @Value("${digital.prison.service.url}") String digitalPrisonServiceBaseUrl,
        @Value("${offender.deletion.review.duration:P1D}") Duration offenderDeletionReviewDuration, NotificationClient client) {
        this.offenderDataCleansedTemplateId = offenderDataCleansedTemplateId;
        this.offenderDeletionReviewDuration = offenderDeletionReviewDuration;
        this.client = client;
        this.enabled = enabled;
        this.digitalPrisonServiceBaseUrl = digitalPrisonServiceBaseUrl;
        this.intentionToDeleteTemplateId = intentionToDeleteTemplateId;
    }


    public void sendIntentionToDeleteEmail(final OffenderDeletionReferral offenderDeletionReferral, final String email) {
        sendEmail(ofIntentionToDelete(offenderDeletionReferral, email, digitalPrisonServiceBaseUrl, intentionToDeleteTemplateId, offenderDeletionReviewDuration));
    }

    public void sendOffenderDataCleansedEmail(final OffenderDeletionReferral offenderDeletionReferral, final String email) {
        sendEmail(ofOffenderDataCleansed(offenderDeletionReferral, email, offenderDataCleansedTemplateId));
    }

    public void sendEmail(final EmailTemplate emailTemplate) {
        if (enabled) send(emailTemplate);
        else log.info("Email notification disabled");
    }

    private void send(EmailTemplate emailTemplate) {
        try {
            client.sendEmail(emailTemplate.getTemplateId(), emailTemplate.getEmail(), emailTemplate.getEmailParameters().toMap(), emailTemplate.getReference());
        } catch (NotificationClientException e) {
            logError(emailTemplate, e);
        } catch (Exception e) {
            log.error("Email notification failed", e);
        }
    }

    private void logError(EmailTemplate emailTemplate, NotificationClientException e) {
        final EmailParameters emailParameters = emailTemplate.getEmailParameters();

        log.error("Gov Notify Error: Unable to send email notification to OMU '{}': '{}', for offender '{}'. Http Response code: '{}'",
            emailParameters.getOmu(), emailTemplate.getEmail(), emailParameters.getNomisNumber(), e.getHttpResult(), e);
    }


}
