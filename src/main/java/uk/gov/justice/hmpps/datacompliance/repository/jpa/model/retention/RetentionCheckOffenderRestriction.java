package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckOffenderRestriction.OFFENDER_RESTRICTION)
public class RetentionCheckOffenderRestriction extends RetentionCheck {

    public static final String OFFENDER_RESTRICTION = "OFFENDER_RESTRICTION";

    private RetentionCheckOffenderRestriction() {
        this(null);
    }

    public RetentionCheckOffenderRestriction(final Status status) {
        super(null, null, OFFENDER_RESTRICTION, status);
    }
}
