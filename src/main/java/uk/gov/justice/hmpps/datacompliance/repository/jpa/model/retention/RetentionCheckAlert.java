package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckAlert.ALERT)
public class RetentionCheckAlert extends RetentionCheck {

    public static final String ALERT = "ALERT";

    private RetentionCheckAlert() {
        this(null);
    }

    public RetentionCheckAlert(final Status status) {
        super(null, null, ALERT, status);
    }
}
