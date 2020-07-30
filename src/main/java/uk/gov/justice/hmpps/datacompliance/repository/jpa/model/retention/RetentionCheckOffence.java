package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckOffence.OFFENCE)
public class RetentionCheckOffence extends RetentionCheck {

    public static final String OFFENCE = "OFFENCE";

    private RetentionCheckOffence() {
        this(null);
    }

    public RetentionCheckOffence(final Status status) {
        super(null, null, OFFENCE, status);
    }
}
