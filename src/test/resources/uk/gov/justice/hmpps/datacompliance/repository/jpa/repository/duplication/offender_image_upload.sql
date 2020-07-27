INSERT INTO offender_image_upload (upload_id, offender_no, offender_image_id, face_id, upload_date_time, batch_id, upload_status, upload_error_reason)
VALUES (1, 'OFFENDER1', 1, '1', to_timestamp('2020-01-01 01:00:00', 'YYYY-MM-DD HH:MI:SS'), 1, 'SUCCESS', null);

INSERT INTO offender_image_upload (upload_id, offender_no, offender_image_id, face_id, upload_date_time, batch_id, upload_status, upload_error_reason)
VALUES (2, 'OFFENDER1', 2, '2', to_timestamp('2020-01-01 02:00:00', 'YYYY-MM-DD HH:MI:SS'), 1, 'ERROR', 'Some error');

INSERT INTO offender_image_upload (upload_id, offender_no, offender_image_id, face_id, upload_date_time, batch_id, upload_status, upload_error_reason)
VALUES (3, 'OFFENDER2', 3, '3', to_timestamp('2020-01-01 03:00:00', 'YYYY-MM-DD HH:MI:SS'), 1, 'DELETED', null);
