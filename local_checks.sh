#!/usr/bin/env bash

echo 'Running OWASP security checks'
./gradlew clean dependencyCheckAnalyze --info

echo 'Running Unit Tests'
./gradlew clean build

echo 'Running Integration Tests'
./gradlew testIntegration 

echo 'Running Message Integration Tests'
./gradlew testMessageIntegration

echo 'Running Message Integration Tests with review period'
./gradlew testMessageIntegrationWithReviewPeriod  

