package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import static lombok.AccessLevel.PRIVATE;

@Entity
@NoArgsConstructor(access = PRIVATE)
@DiscriminatorValue(RetentionCheckIdDataDuplicate.DATA_DUPLICATE_ID)
public class RetentionCheckIdDataDuplicate extends RetentionCheckDataDuplicate {

    public static final String DATA_DUPLICATE_ID = "DATA_DUPLICATE_ID";

    public RetentionCheckIdDataDuplicate(final Status status) {
        super(DATA_DUPLICATE_ID, status);
    }
}
