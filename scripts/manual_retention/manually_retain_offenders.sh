#!/usr/bin/env bash

for ARGUMENT in "$@"
do
   KEY=$(echo $ARGUMENT | cut -f1 -d=)

   KEY_LENGTH=${#KEY}
   VALUE="${ARGUMENT:$KEY_LENGTH+1}"

   export "$KEY"="$VALUE"
done

while IFS=, read -r noms_id retention_reason_code reasonDetails; do

  curl --request PUT "https://prison-data-compliance-dev.prison.service.justice.gov.uk/retention/offenders/${noms_id}" \
  --header 'Content-Type: application/json' \
  --header "Authorization: Bearer $TOKEN" \
  --data-raw "{
      \"retentionReasons\": [
          {
              \"reasonCode\": \"${retention_reason_code}\",
              \"reasonDetails\": \"${reasonDetails}\"
          }
      ]
  }"
done <offender_retentions.csv


