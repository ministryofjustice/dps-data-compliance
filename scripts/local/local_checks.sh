#!/usr/bin/env bash

println () {
  printf '%*s\n' "${COLUMNS:-$(tput cols)}" '' | tr ' ' -
  echo $1
  printf '%*s\n' "${COLUMNS:-$(tput cols)}" '' | tr ' ' -
  }

println 'Running Unit Tests'
./gradlew clean build

println 'Running OWASP security checks'
./gradlew clean dependencyCheckAnalyze --info

println 'Running Integration Tests'
./gradlew testIntegration 

println 'Running Message Integration Tests'
./gradlew testMessageIntegration

println 'Running Message Integration Tests with review period'
./gradlew testMessageIntegrationWithReviewPeriod  

println 'Running Message Integration Tests with review period'
./gradlew testMessageIntegrationWithReviewPeriod

println 'Running DLQ tests'
./gradlew testDlq
