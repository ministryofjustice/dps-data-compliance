package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import static lombok.AccessLevel.PRIVATE;

@Entity
@NoArgsConstructor(access = PRIVATE)
@DiscriminatorValue(RetentionCheckDatabaseDataDuplicate.DATA_DUPLICATE_DB)
public class RetentionCheckDatabaseDataDuplicate extends RetentionCheckDataDuplicate {

    public static final String DATA_DUPLICATE_DB = "DATA_DUPLICATE_DB";

    public RetentionCheckDatabaseDataDuplicate(final Status status){
        super(DATA_DUPLICATE_DB, status);
    }
}
