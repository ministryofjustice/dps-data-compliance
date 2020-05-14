package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckPathfinder.PATHFINDER_REFERRAL)
public class RetentionCheckPathfinder extends RetentionCheck {

    public static final String PATHFINDER_REFERRAL = "PATHFINDER_REFERRAL";

    private RetentionCheckPathfinder() {
        this(null);
    }

    public RetentionCheckPathfinder(final Status status) {
        super(null, null, PATHFINDER_REFERRAL, status);
    }
}
