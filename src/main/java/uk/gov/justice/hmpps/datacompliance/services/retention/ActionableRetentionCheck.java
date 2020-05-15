package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;

/**
 * A wrapper class for the RetentionCheck,
 * allowing a process to be triggered once
 * database entities have been persisted and
 * generated ids are made available.
 */
@RequiredArgsConstructor
public class ActionableRetentionCheck {

    @Getter
    private final RetentionCheck retentionCheck;

    private PendingCheck pendingCheck;

    public ActionableRetentionCheck setPendingCheck(final PendingCheck pendingCheck) {
        this.pendingCheck = pendingCheck;
        return this;
    }

    public void triggerPendingCheck() {
        if (pendingCheck != null) {
            pendingCheck.triggerCheck(retentionCheck);
        }
    }

    @FunctionalInterface
    public interface PendingCheck {
        void triggerCheck(RetentionCheck retentionCheck);
    }
}
