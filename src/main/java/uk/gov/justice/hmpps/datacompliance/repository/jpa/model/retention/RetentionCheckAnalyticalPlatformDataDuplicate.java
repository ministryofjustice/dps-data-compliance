package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import static lombok.AccessLevel.PRIVATE;

@Entity
@NoArgsConstructor(access = PRIVATE)
@DiscriminatorValue(RetentionCheckAnalyticalPlatformDataDuplicate.DATA_DUPLICATE_AP)
public class RetentionCheckAnalyticalPlatformDataDuplicate extends RetentionCheckDataDuplicate {

    public static final String DATA_DUPLICATE_AP = "DATA_DUPLICATE_AP";

    public RetentionCheckAnalyticalPlatformDataDuplicate(final Status status) {
        super(DATA_DUPLICATE_AP, status);
    }
}
