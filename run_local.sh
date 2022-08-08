#!/bin/bash

println () {
  printf '%*s\n' "${COLUMNS:-$(tput cols)}" '' | tr ' ' -
  echo $1
  printf '%*s\n' "${COLUMNS:-$(tput cols)}" '' | tr ' ' -
  }

healthyStatus='"sqs": "running"'

println "BUILDING PROJECT"
./gradlew --no-daemon assemble

println "STARTING UP DEPENDENCIES"
docker-compose up --scale dps-data-compliance=0 -d

while [[ $(curl -sb -H "Accept: application/json" "http://localhost:4566/health") != *"$healthyStatus"* ]]; do sleep 1; done

println "ALL DEPENDENCIES ARE RUNNING"

println "STARTING DPS-DATA-COMPLAINCE WITH THE 'DEV' PROFILE"
./gradlew bootRun --args='--spring.profiles.active=dev'
