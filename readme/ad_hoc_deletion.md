[< Back](../README.md)
---
## Ad Hoc Deletion

The Data Compliance Service is normally triggered
by a scheduled cron and all offenders considered 
where the sentence end date falls within a given
window. It is possible to request the deletion of
just a single offender by using the following script.

The script submits an SQS event that the Data Compliance
Service will pick up and parse as an ad hoc offender
deletion request. The same checks are performed.

[Ad Hoc Offender Deletion Script](./ad-hoc-offender-deletion.sh)
