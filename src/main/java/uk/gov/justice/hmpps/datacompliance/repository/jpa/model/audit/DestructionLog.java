package uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestructionLog implements OffenderEntity {

    @Id
    private String offenderNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String typeOfRecordDestroyed;
    private LocalDateTime destructionDate;
    private String methodOfDestruction;
    private String authorisationOfDestruction;




    @Override
    public OffenderNumber getOffenderNumber() {
        return new OffenderNumber(offenderNumber);
    }
}
