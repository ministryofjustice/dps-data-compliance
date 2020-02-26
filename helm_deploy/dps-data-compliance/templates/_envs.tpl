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

  - name: SPRING_PROFILES_ACTIVE
    value: "sns"

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

  - name: INBOUND_REFERRAL_SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: offender-pending-deletion-sqs
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

{{- end -}}
