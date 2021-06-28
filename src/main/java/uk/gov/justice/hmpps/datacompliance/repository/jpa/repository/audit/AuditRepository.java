package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.DestructionLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends CrudRepository<DestructionLog, String> {

    @Query(value = "SELECT DISTINCT odr.offender_no AS offender_number," +
        " odr.first_name AS first_name," +
        " odr.middle_name AS middle_name," +
        " odr.last_name AS last_name," +
        " odr.birth_date AS date_of_birth," +
        " 'NOMIS record' AS type_of_record_destroyed," +
        " rr.resolution_date_time AS destruction_date," +
        " 'NOMIS database deletion' AS method_of_destruction," +
        " 'MOJ' AS authorisation_of_destruction" +
        " FROM offender_deletion_referral odr" +
        " INNER JOIN referred_offender_alias roa" +
        " ON roa.referral_id = odr.referral_id" +
        " INNER JOIN referral_resolution rr" +
        " ON rr.referral_id = odr.referral_id " +
        " WHERE rr.resolution_status = 'DELETED'", nativeQuery = true)
    public List<DestructionLog> findDestructionLog();


    @Query(value = "SELECT DISTINCT odr.offender_no AS offender_number," +
        " odr.first_name AS first_name," +
        " odr.middle_name AS middle_name," +
        " odr.last_name AS last_name," +
        " odr.birth_date AS date_of_birth," +
        " 'NOMIS record' AS type_of_record_destroyed," +
        " rr.resolution_date_time AS destruction_date," +
        " 'NOMIS database deletion' AS method_of_destruction," +
        " 'MOJ' AS authorisation_of_destruction" +
        " FROM offender_deletion_referral odr" +
        " INNER JOIN referred_offender_alias roa" +
        " ON roa.referral_id = odr.referral_id" +
        " INNER JOIN referral_resolution rr" +
        " ON rr.referral_id = odr.referral_id " +
        " WHERE rr.resolution_status = 'DELETED'" +
        " AND rr.resolution_date_time >= (:fromDate)" +
        " AND rr.resolution_date_time <= (:toDate)", nativeQuery = true)
    public List<DestructionLog> findDestructionLogBetweenDates(LocalDateTime fromDate, LocalDateTime toDate);




}
