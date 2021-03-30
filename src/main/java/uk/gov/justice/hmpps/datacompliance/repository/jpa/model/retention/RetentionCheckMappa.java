package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckMappa.MAPPA_REFERRAL)
public class RetentionCheckMappa extends RetentionCheck {

    public static final String MAPPA_REFERRAL = "MAPPA_REFERRAL";

    private RetentionCheckMappa() {
        this(null);
    }

    public RetentionCheckMappa(final Status status) {
        super(null, null, MAPPA_REFERRAL, status);
    }
}
