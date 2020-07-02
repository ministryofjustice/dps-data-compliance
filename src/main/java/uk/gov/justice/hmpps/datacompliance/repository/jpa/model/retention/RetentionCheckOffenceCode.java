package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckOffenceCode.OFFENCE_CODE)
public class RetentionCheckOffenceCode extends RetentionCheck {

    public static final String OFFENCE_CODE = "OFFENCE_CODE";

    private RetentionCheckOffenceCode() {
        this(null);
    }

    public RetentionCheckOffenceCode(final Status status) {
        super(null, null, OFFENCE_CODE, status);
    }
}
