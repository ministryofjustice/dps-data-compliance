[< Back](../README.md)
---

## Configuring Deletions

### Scheduling deletions of offenders with 1 booking after 7 years from their end of sentence

In order to schedule a deletion, a cron expression must be specified which will configure an instance of a CronTrigger.
This will trigger the cron job to run at the specified schedule.

The job will result in the deletion of the offender's data from nomis **excluding** the `Base Record`. The base record
consists of data from the following tables:

```
OFFENDERS
OFFENDER_IDENTIFIER
OFFENDER_IMAGES
OFFENDER_IDENTIFYING_MARKS
OFFENDER_ALERTS
OFFENDER_EXTERNAL_MOVEMENTS
OFFENDER_BOOKINGS
OFFENDER_BOOKING_DETAILS
OFFENDER_PHYSICAL_ATTRIBUTES
OFFENDER_PROFILE_DETAILS
OFFENDER_BELIEFS
```

Below is an example of setting the job to run at 06:30 PM, Monday through Friday

```yaml
OFFENDER_DELETION_CRON: "0 30 18 ? * MON-FRI *"
```

The following must also be configured:

- Initial start window (when the offender is due for deletion)
- The duration period between the start window and end window
- The limit of offenders referred per iteration

Examples below:

```yaml
OFFENDER_DELETION_INITIAL_WINDOW_START: "2000-04-20T00:00:00"
OFFENDER_DELETION_WINDOW_LENGTH: "P6000D"
OFFENDER_DELETION_REFERRAL_LIMIT: 200
```

### Scheduling deletions of offenders with no bookings

To schedule a **complete deletion of all offender data** from nomis of offenders with no bookings you
must configure a cron schedule and a deletion limit (per iteration)

Examples below:

```yaml
OFFENDER_NO_BOOKING_DELETION_CRON: "0 0 16 ? * MON-FRI *"
OFFENDER_NO_BOOKING_DELETION_LIMIT: 200
```

### Scheduling deletions of deceased offenders

To schedule a **complete deletion of all offender data** of offenders who are deceased over 20 years 
you must configure a cron schedule and a deletion limit (per iteration)

Examples below:

```yaml
DECEASED_OFFENDER_DELETION_CRON: "0 30 19 ? * MON *"
DECEASED_OFFENDER_DELETION_LIMIT: 200
```



