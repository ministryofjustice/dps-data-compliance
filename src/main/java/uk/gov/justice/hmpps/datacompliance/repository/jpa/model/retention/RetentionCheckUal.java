package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckUal.UAL)
public class RetentionCheckUal extends RetentionCheck {

    public static final String UAL = "UAL";

    private RetentionCheckUal() {
        this(null);
    }

    public RetentionCheckUal(final Status status) {
        super(null, null, UAL, status);
    }
}
