package uk.gov.justice.hmpps.datacompliance.client.govnotify;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;

import java.time.Duration;
import java.util.StringJoiner;

@Data
@Builder
@RequiredArgsConstructor
public class EmailTemplate {

    private final String templateId;
    private final String email;
    private final EmailParameters emailParameters;
    private final String reference;



    public static EmailTemplate ofIntentionToDelete(final OffenderDeletionReferral offenderDeletionReferral, final String email, final String digitalServiceUrl, final String intentionToDeleteTemplateId, Duration reviewDuration) {
        return EmailTemplate.builder()
            .templateId(intentionToDeleteTemplateId)
            .email(email)
            .emailParameters(buildIntentionToDeleteParameters(offenderDeletionReferral, digitalServiceUrl, reviewDuration))
            .reference(buildReference(offenderDeletionReferral))
            .build();
    }

    public static EmailTemplate ofOffenderDataCleansed(final OffenderDeletionReferral offenderDeletionReferral, final String email, final String offenderDataCleansedTemplateId) {
        return EmailTemplate.builder()
            .templateId(offenderDataCleansedTemplateId)
            .email(email)
            .emailParameters(buildOffenderDataCleansedParameters(offenderDeletionReferral))
            .reference(buildReference(offenderDeletionReferral))
            .build();
    }


    private static EmailParameters buildIntentionToDeleteParameters(final OffenderDeletionReferral offenderDeletionReferral, final String digitalServiceUrl, Duration reviewDuration) {
        final var offenderNumber = offenderDeletionReferral.getOffenderNumber();

        return EmailParameters.builder()
            .omu(offenderDeletionReferral.getAgencyLocationId())
            .offenderName(buildName(offenderDeletionReferral))
            .nomisNumber(offenderNumber.getOffenderNumber())
            .dpsRetentionUrl(buildDpsLink(offenderNumber.getOffenderNumber(), digitalServiceUrl))
            .reviewPeriodDuration(buildReviewPeriodDurationString(reviewDuration))
            .build();
    }

    private static EmailParameters buildOffenderDataCleansedParameters(final OffenderDeletionReferral offenderDeletionReferral) {
        final var offenderNumber = offenderDeletionReferral.getOffenderNumber();

        return EmailParameters.builder()
            .omu(offenderDeletionReferral.getAgencyLocationId())
            .offenderName(buildName(offenderDeletionReferral))
            .nomisNumber(offenderNumber.getOffenderNumber())
            .build();
    }

    private static String buildReference(final OffenderDeletionReferral offenderDeletionReferral) {
        return new StringBuilder()
            .append("Ref-")
            .append(offenderDeletionReferral.getOffenderNo())
            .append("-")
            .append(offenderDeletionReferral.getAgencyLocationId())
            .toString();
    }


    private static String buildReviewPeriodDurationString(Duration reviewDuration) {
        final int addDays = reviewDuration.toHoursPart() > 0 ? 1 : 0;
        return (reviewDuration.toDaysPart() + addDays) + " days";
    }

    private static String buildName(final OffenderDeletionReferral offenderDeletionReferral) {
        return new StringJoiner(" ")
            .add(offenderDeletionReferral.getFirstName())
            .add(offenderDeletionReferral.getMiddleName())
            .add(offenderDeletionReferral.getLastName())
            .toString();
    }

    private static String buildDpsLink(final String offenderNumber, final String digitalServiceUrl) {
        return digitalServiceUrl + "/offenders/" + offenderNumber + "/retention-reasons";
    }
}
