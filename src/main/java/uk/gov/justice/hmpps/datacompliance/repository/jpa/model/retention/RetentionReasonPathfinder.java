package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionReasonPathfinder.PATHFINDER_REFERRAL)
public class RetentionReasonPathfinder extends RetentionReason {

    public static final String PATHFINDER_REFERRAL = "PATHFINDER_REFERRAL";

    public RetentionReasonPathfinder() {
        super(null, null, PATHFINDER_REFERRAL);
    }
}
