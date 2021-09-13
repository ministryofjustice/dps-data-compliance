{{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: HMPPS_AUTH_BASE_URL
    value: "{{ .Values.env.HMPPS_AUTH_BASE_URL }}"

  - name: PRISON_API_BASE_URL
    value: "{{ .Values.env.PRISON_API_BASE_URL }}"

  - name: PATHFINDER_API_BASE_URL
    value: "{{ .Values.env.PATHFINDER_API_BASE_URL }}"

  - name: COMMUNITY_API_BASE_URL
    value: "{{ .Values.env.COMMUNITY_API_BASE_URL }}"

  - name: PRISON_REGISTER_BASE_URL
    value: "{{ .Values.env.PRISON_REGISTER_BASE_URL }}"

  - name: DIGITAL_PRISON_SERVICE_URL
    value: "{{ .Values.env.DIGITAL_PRISON_SERVICE_URL }}"

  - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
    value: "{{ .Values.env.SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI }}"

  - name: OFFENDER_RETENTION_IMAGE_DUPLICATE_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_IMAGE_DUPLICATE_CHECK_ENABLED }}"

  - name: OFFENDER_RETENTION_DATA_DUPLICATE_ID_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_DATA_DUPLICATE_ID_CHECK_ENABLED }}"

  - name: OFFENDER_RETENTION_DATA_DUPLICATE_DB_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_DATA_DUPLICATE_DB_CHECK_ENABLED }}"

  - name: OFFENDER_RETENTION_DATA_DUPLICATE_AP_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_DATA_DUPLICATE_AP_CHECK_ENABLED }}"

  - name: OFFENDER_RETENTION_FALSE_POSITIVE_DUPLICATE_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_FALSE_POSITIVE_DUPLICATE_CHECK_ENABLED }}"

  - name: DELETION_GRANT_ENABLED
    value: "{{ .Values.env.DELETION_GRANT_ENABLED }}"

  - name: IMAGE_RECOGNITION_DELETION_ENABLED
    value: "{{ .Values.env.IMAGE_RECOGNITION_DELETION_ENABLED }}"

  - name: NOTIFY_ENABLED
    value: "{{ .Values.env.NOTIFY_ENABLED }}"

  - name: OFFENDER_DELETION_REVIEW_REQUIRED
    value: "{{ .Values.env.OFFENDER_DELETION_REVIEW_REQUIRED }}"

  {{- if .Values.env.IMAGE_RECOGNITION_MIGRATION_CRON }}
  - name: IMAGE_RECOGNITION_MIGRATION_CRON
    value: "{{ .Values.env.IMAGE_RECOGNITION_MIGRATION_CRON }}"
  {{- end }}

  {{- if .Values.env.IMAGE_RECOGNITION_PROVIDER }}
  - name: IMAGE_RECOGNITION_PROVIDER
    value: "{{ .Values.env.IMAGE_RECOGNITION_PROVIDER }}"
  {{- end }}

  {{- if .Values.env.IMAGE_RECOGNITION_UPLOAD_PERMITS_PER_SECOND }}
  - name: IMAGE_RECOGNITION_UPLOAD_PERMITS_PER_SECOND
    value: "{{ .Values.env.IMAGE_RECOGNITION_UPLOAD_PERMITS_PER_SECOND }}"
  {{- end }}

  {{- if .Values.env.PRISON_API_OFFENDER_IDS_PAGE_LIMIT }}
  - name: PRISON_API_OFFENDER_IDS_PAGE_LIMIT
    value: "{{ .Values.env.PRISON_API_OFFENDER_IDS_PAGE_LIMIT }}"
  {{- end }}

  {{- if .Values.env.PRISON_API_OFFENDER_IDS_TOTAL_PAGES }}
  - name: PRISON_API_OFFENDER_IDS_TOTAL_PAGES
    value: "{{ .Values.env.PRISON_API_OFFENDER_IDS_TOTAL_PAGES }}"
  {{- end }}

  {{- if .Values.env.PRISON_API_OFFENDER_IDS_INITIAL_OFFSET }}
  - name: PRISON_API_OFFENDER_IDS_INITIAL_OFFSET
    value: "{{ .Values.env.PRISON_API_OFFENDER_IDS_INITIAL_OFFSET }}"
  {{- end }}

  {{- if .Values.env.PRISON_API_OFFENDER_IDS_ITERATION_THREADS }}
  - name: PRISON_API_OFFENDER_IDS_ITERATION_THREADS
    value: "{{ .Values.env.PRISON_API_OFFENDER_IDS_ITERATION_THREADS }}"
  {{- end }}


  {{- if .Values.env.OFFENDER_DELETION_CRON }}
  - name: OFFENDER_DELETION_CRON
    value: "{{ .Values.env.OFFENDER_DELETION_CRON }}"
  {{- end }}

  {{- if .Values.env.OFFENDER_DELETION_INITIAL_WINDOW_START }}
  - name: OFFENDER_DELETION_INITIAL_WINDOW_START
    value: "{{ .Values.env.OFFENDER_DELETION_INITIAL_WINDOW_START }}"
  {{- end }}

  {{- if .Values.env.OFFENDER_DELETION_WINDOW_LENGTH }}
  - name: OFFENDER_DELETION_WINDOW_LENGTH
    value: "{{ .Values.env.OFFENDER_DELETION_WINDOW_LENGTH }}"
  {{- end }}

  {{- if .Values.env.OFFENDER_DELETION_REFERRAL_LIMIT }}
  - name: OFFENDER_DELETION_REFERRAL_LIMIT
    value: "{{ .Values.env.OFFENDER_DELETION_REFERRAL_LIMIT }}"
  {{- end }}


  {{- if .Values.env.OFFENDER_DELETION_REVIEW_DURATION }}
  - name: OFFENDER_DELETION_REVIEW_DURATION
    value: "{{ .Values.env.OFFENDER_DELETION_REVIEW_DURATION }}"
  {{- end }}

  {{- if .Values.env.OFFENDER_DELETION_LIMIT }}
  - name: OFFENDER_DELETION_LIMIT
    value: "{{ .Values.env.OFFENDER_DELETION_LIMIT }}"
  {{- end }}

  {{- if .Values.env.DECEASED_OFFENDER_DELETION_CRON }}
  - name: DECEASED_OFFENDER_DELETION_CRON
    value: "{{ .Values.env.DECEASED_OFFENDER_DELETION_CRON }}"
  {{- end }}

  {{- if .Values.env.DECEASED_OFFENDER_DELETION_LIMIT }}
  - name: DECEASED_OFFENDER_DELETION_LIMIT
    value: "{{ .Values.env.DECEASED_OFFENDER_DELETION_LIMIT }}"
  {{- end }}

  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_username

  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_password

  - name: DB_NAME
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_name

  - name: DB_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: rds_instance_endpoint

  - name: APP_DB_URL
    value: "jdbc:postgresql://$(DB_ENDPOINT)/$(DB_NAME)?sslmode=verify-full"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: DPS_DATA_COMPLIANCE_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: DPS_DATA_COMPLIANCE_CLIENT_ID

  - name: DPS_DATA_COMPLIANCE_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: DPS_DATA_COMPLIANCE_CLIENT_SECRET

  - name: IMAGE_RECOGNITION_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: IMAGE_RECOGNITION_AWS_ACCESS_KEY_ID

  - name: IMAGE_RECOGNITION_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: IMAGE_RECOGNITION_AWS_SECRET_ACCESS_KEY

  - name: NOTIFY_API_KEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: NOTIFY_API_KEY

  - name: HMPPS_SQS_TOPICS_DATACOMPLIANCEEVENTS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: access_key_id

  - name: HMPPS_SQS_TOPICS_DATACOMPLIANCEEVENTS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: secret_access_key

  - name: HMPPS_SQS_TOPICS_DATACOMPLIANCEEVENTS_ARN
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: topic_arn

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCERESPONSE_QUEUE_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCERESPONSE_QUEUE_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCERESPONSE_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs
        key: sqs_dc_resp_name

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCERESPONSE_DLQ_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs-dl
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCERESPONSE_DLQ_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs-dl
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCERESPONSE_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs-dl
        key: sqs_dc_resp_name

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_QUEUE_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_QUEUE_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: sqs_dc_req_name

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_QUEUE_URL
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: sqs_dc_req_url

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_DLQ_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs-dl
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_DLQ_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs-dl
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_DATACOMPLIANCEREQUEST_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs-dl
        key: sqs_dc_req_name

  {{- if .Values.env.DUPLICATE_DETECTION_PROVIDER }}

  - name: DUPLICATE_DETECTION_PROVIDER
    value: "{{ .Values.env.DUPLICATE_DETECTION_PROVIDER }}"

  - name: DUPLICATE_DETECTION_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-ap-user
        key: data_compliance_ap_access_key_id

  - name: DUPLICATE_DETECTION_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-ap-user
        key: data_compliance_ap_secret_access_key

  - name: DUPLICATE_DETECTION_ROLE_ARN
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: DUPLICATE_DETECTION_ROLE_ARN

  - name: DUPLICATE_DETECTION_ATHENA_OUTPUT_LOCATION
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: DUPLICATE_DETECTION_ATHENA_OUTPUT_LOCATION

  {{- end }}

{{- end -}}
