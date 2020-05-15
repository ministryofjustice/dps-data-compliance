package uk.gov.justice.hmpps.datacompliance.repository.jpa.model;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

/**
 * A database entity that is linked to an offender.
 */
public interface OffenderEntity {
    OffenderNumber getOffenderNumber();
}
