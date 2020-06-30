package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RetentionCheckFreeTextSearch.FREE_TEXT_SEARCH)
public class RetentionCheckFreeTextSearch extends RetentionCheck {

    public static final String FREE_TEXT_SEARCH = "FREE_TEXT_SEARCH";

    private RetentionCheckFreeTextSearch() {
        this(null);
    }

    public RetentionCheckFreeTextSearch(final Status status) {
        super(null, null, FREE_TEXT_SEARCH, status);
    }
}
