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

  - name: ELITE2_API_BASE_URL
    value: "{{ .Values.env.ELITE2_API_BASE_URL }}"

  - name: OAUTH_API_BASE_URL
    value: "{{ .Values.env.OAUTH_API_BASE_URL }}"

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

  {{- if .Values.env.OUTBOUND_DELETION_SQS_PROVIDER }}
  - name: OUTBOUND_DELETION_SQS_PROVIDER
    value: "{{ .Values.env.OUTBOUND_DELETION_SQS_PROVIDER }}"
  {{- end }}

  {{- if .Values.env.INBOUND_REFERRAL_SQS_PROVIDER }}
  - name: INBOUND_REFERRAL_SQS_PROVIDER
    value: "{{ .Values.env.INBOUND_REFERRAL_SQS_PROVIDER }}"
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

  - name: INBOUND_REFERRAL_SQS_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs
        key: access_key_id

  - name: INBOUND_REFERRAL_SQS_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs
        key: secret_access_key

  - name: INBOUND_REFERRAL_SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs
        key: sqs_opd_name

  - name: INBOUND_REFERRAL_SQS_DLQ_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs-dl
        key: access_key_id

  - name: INBOUND_REFERRAL_SQS_DLQ_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs-dl
        key: secret_access_key

  - name: INBOUND_REFERRAL_SQS_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs-dl
        key: sqs_opd_name

  - name: OUTBOUND_DELETION_SQS_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs
        key: access_key_id

  - name: OUTBOUND_DELETION_SQS_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs
        key: secret_access_key

  - name: OUTBOUND_DELETION_SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs
        key: sqs_odg_name

  - name: OUTBOUND_DELETION_SQS_QUEUE_URL
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs
        key: sqs_odg_url

  - name: OUTBOUND_DELETION_SQS_DLQ_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs-dl
        key: access_key_id

  - name: OUTBOUND_DELETION_SQS_DLQ_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs-dl
        key: secret_access_key

  - name: OUTBOUND_DELETION_SQS_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: offender-deletion-granted-sqs-dl
        key: sqs_odg_name


{{- end -}}
