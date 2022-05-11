package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.audit.RetainedOffender;

@Repository
public interface RetainedOffenderRepository extends CrudRepository<RetainedOffender, String> {

    @Query(value = """
        SELECT DISTINCT 
        odr.offender_no AS offender_number, 
        odr.first_name AS first_name, 
        odr.middle_name AS middle_name, 
        odr.last_name AS last_name, 
        odr.birth_date AS date_of_birth, 
        odr.agency_location_id AS last_known_omu, 
        string_agg(distinct(rc.check_type), ',') AS positive_retention_checks 
        FROM offender_deletion_referral odr 
        INNER JOIN referred_offender_alias roa  
        ON roa.referral_id = odr.referral_id 
        INNER JOIN referral_resolution rr  
        ON rr.referral_id = odr.referral_id 
        INNER JOIN retention_check rc  
        ON rc.resolution_id = rr.resolution_id 
        WHERE rc.check_status = 'RETENTION_REQUIRED' 
        GROUP BY offender_number, first_name, middle_name, last_name, date_of_birth, last_known_omu;""", nativeQuery = true)
    Page<RetainedOffender> findRetainedOffenders(final Pageable pageable);


    @Query(value = """
        SELECT DISTINCT 
        odr.offender_no AS offender_number, 
        odr.first_name AS first_name, 
        odr.middle_name AS middle_name, 
        odr.last_name AS last_name, 
        odr.birth_date AS date_of_birth, 
        odr.agency_location_id AS last_known_omu, 
        string_agg(distinct(rc.check_type), ',') AS positive_retention_checks 
        FROM offender_deletion_referral odr 
        INNER JOIN referred_offender_alias roa  
        ON roa.referral_id = odr.referral_id 
        INNER JOIN referral_resolution rr  
        ON rr.referral_id = odr.referral_id 
        INNER JOIN retention_check rc  
        ON rc.resolution_id = rr.resolution_id 
        WHERE rc.check_status = 'RETENTION_REQUIRED'
        AND rc.check_type IN ('DATA_DUPLICATE_AP', 'DATA_DUPLICATE_ID', 'IMAGE_DUPLICATE')
        GROUP BY offender_number, first_name, middle_name, last_name, date_of_birth, last_known_omu;""", nativeQuery = true)
    Page<RetainedOffender> findRetainedOffenderDuplicates(final Pageable pageable);

}
