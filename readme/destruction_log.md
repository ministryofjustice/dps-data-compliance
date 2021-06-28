[< Back](../README.md)
---
## Destruction Log

A copy of the destruction log can be obtained from
the system at any time by making the following query
on the database:

```postgresql
SELECT DISTINCT
    odr.offender_no AS offender_number,
    odr.first_name AS offender_forename,
    odr.middle_name AS offender_middle_name,
    odr.last_name AS offender_surname,
    odr.birth_date AS offender_date_of_birth,
    'NOMIS record' AS type_of_record_destroyed,
    rr.resolution_date_time AS destruction_date,
    'NOMIS database deletion' AS method_of_destruction,
    'MOJ' AS authorisation_of_destruction
FROM offender_deletion_referral odr
INNER JOIN referred_offender_alias roa 
ON roa.referral_id = odr.referral_id 
INNER JOIN referral_resolution rr
ON rr.referral_id = odr.referral_id 
WHERE rr.resolution_status = 'DELETED';
```

The exact wording of the `type_of_record_destroyed`,
`method_of_destruction` and `authorisation_of_destruction` fields
are still to be confirmed (see GDPR-182).
