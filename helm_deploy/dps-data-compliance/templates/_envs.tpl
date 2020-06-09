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

  - name: OAUTH_API_BASE_URL
    value: "{{ .Values.env.OAUTH_API_BASE_URL }}"

  - name: ELITE2_API_BASE_URL
    value: "{{ .Values.env.ELITE2_API_BASE_URL }}"

  - name: PATHFINDER_API_BASE_URL
    value: "{{ .Values.env.PATHFINDER_API_BASE_URL }}"

  - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
    value: "{{ .Values.env.SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI }}"

  - name: OFFENDER_RETENTION_DATA_DUPLICATE_ID_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_DATA_DUPLICATE_ID_CHECK_ENABLED }}"

  - name: OFFENDER_RETENTION_DATA_DUPLICATE_DB_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_DATA_DUPLICATE_DB_CHECK_ENABLED }}"

  - name: OFFENDER_RETENTION_DATA_DUPLICATE_AP_CHECK_ENABLED
    value: "{{ .Values.env.OFFENDER_RETENTION_DATA_DUPLICATE_AP_CHECK_ENABLED }}"

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

  {{- if .Values.env.ELITE2_API_OFFENDER_IDS_TOTAL_PAGES }}
  - name: ELITE2_API_OFFENDER_IDS_TOTAL_PAGES
    value: "{{ .Values.env.ELITE2_API_OFFENDER_IDS_TOTAL_PAGES }}"
  {{- end }}

  {{- if .Values.env.ELITE2_API_OFFENDER_IDS_INITIAL_OFFSET }}
  - name: ELITE2_API_OFFENDER_IDS_INITIAL_OFFSET
    value: "{{ .Values.env.ELITE2_API_OFFENDER_IDS_INITIAL_OFFSET }}"
  {{- end }}

  {{- if .Values.env.ELITE2_API_OFFENDER_IDS_ITERATION_THREADS }}
  - name: ELITE2_API_OFFENDER_IDS_ITERATION_THREADS
    value: "{{ .Values.env.ELITE2_API_OFFENDER_IDS_ITERATION_THREADS }}"
  {{- end }}

  {{- if .Values.env.SNS_PROVIDER }}
  - name: SNS_PROVIDER
    value: "{{ .Values.env.SNS_PROVIDER }}"
  {{- end }}

  {{- if .Values.env.DATA_COMPLIANCE_REQUEST_SQS_PROVIDER }}
  - name: DATA_COMPLIANCE_REQUEST_SQS_PROVIDER
    value: "{{ .Values.env.DATA_COMPLIANCE_REQUEST_SQS_PROVIDER }}"
  {{- end }}

  {{- if .Values.env.DATA_COMPLIANCE_RESPONSE_SQS_PROVIDER }}
  - name: DATA_COMPLIANCE_RESPONSE_SQS_PROVIDER
    value: "{{ .Values.env.DATA_COMPLIANCE_RESPONSE_SQS_PROVIDER }}"
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

  - name: SNS_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: access_key_id

  - name: SNS_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: secret_access_key

  - name: SNS_TOPIC_ARN
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: topic_arn

  - name: DATA_COMPLIANCE_RESPONSE_SQS_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs
        key: access_key_id

  - name: DATA_COMPLIANCE_RESPONSE_SQS_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs
        key: secret_access_key

  - name: DATA_COMPLIANCE_RESPONSE_SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs
        key: sqs_dc_resp_name

  - name: DATA_COMPLIANCE_RESPONSE_SQS_DLQ_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs-dl
        key: access_key_id

  - name: DATA_COMPLIANCE_RESPONSE_SQS_DLQ_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs-dl
        key: secret_access_key

  - name: DATA_COMPLIANCE_RESPONSE_SQS_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-response-sqs-dl
        key: sqs_dc_resp_name

  - name: DATA_COMPLIANCE_REQUEST_SQS_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: access_key_id

  - name: DATA_COMPLIANCE_REQUEST_SQS_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: secret_access_key

  - name: DATA_COMPLIANCE_REQUEST_SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: sqs_dc_req_name

  - name: DATA_COMPLIANCE_REQUEST_SQS_QUEUE_URL
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs
        key: sqs_dc_req_url

  - name: DATA_COMPLIANCE_REQUEST_SQS_DLQ_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs-dl
        key: access_key_id

  - name: DATA_COMPLIANCE_REQUEST_SQS_DLQ_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs-dl
        key: secret_access_key

  - name: DATA_COMPLIANCE_REQUEST_SQS_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: data-compliance-request-sqs-dl
        key: sqs_dc_req_name

{{- end -}}
