[< Back](../README.md)
---
## Deletion Event Queues

The DPS Data Compliance Service communicates with Elite2 API to retrieve referrals
for offender deletion and to send commands to delete offender data.

This is done using two SQS queues, one inbound and one outbound.

The Data Compliance Service requests, via an async API, that Elite2 populates the
offender pending deletion queue with a batch of referrals.  The Data Compliance service 
consumes this queue and if it cannot find any reason why the offender data cannot be
deleted, it will then publish a new event on the offender deletion granted queue which
Elite2 will consume and process the deletion.

For consistency with other DPS queues, messages have an `eventType` message attribute.
The messages received from Elite2 API for deletion referral will have an `eventType`
of either `DATA_COMPLIANCE_OFFENDER-PENDING-DELETION` or 
`DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-COMPLETE`.  The `-COMPLETE` message is sent
by Elite2 API once it has finished adding messages to the queue for that batch.  This
allows the Data Compliance Service to check that the process associated with a given
async API request has been completed.

### Setup

To demonstrate locally how the event publishing and subscription
works, we can set up services and a Localstack instance using docker:

```bash
TMPDIR=/private$TMPDIR docker-compose up
```

### Localstack Queues
The localstack section in docker-compose.yml ensures that the 
`localstack/setup-queues.sh` file and associated queue attributes files
are copied into an auto-execution directory in the container.

### Publishing to the queues

(Warning, if configured, this will prompt Elite2 to delete the Offender provided)

The following command can be run after the docker containers have started up
in order to simulate a deletion (replacing `SOME_OFFENDER_ID_DISPLAY` with a
valid offender number:

```bash
aws --endpoint-url=http://localhost:4576 sqs send-message \
    --queue-url http://localstack:4576/queue/outbound_deletion_queue \
    --message-body '{"offenderIdDisplay":"A1234AA"}' \
    --message-attributes "eventType={StringValue=DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED,DataType=String}"
```

The following queue publishing will cause the Data Compliance Service to analyse
the offender for deletion eligibility:

```bash
aws --endpoint-url=http://localhost:4576 sqs send-message \
    --queue-url http://localstack:4576/queue/inbound_referral_queue \
    --message-body '{"offenderIdDisplay":"A1234AA"}' \
    --message-attributes "eventType={StringValue=DATA_COMPLIANCE_OFFENDER-PENDING-DELETION,DataType=String}"
```

### Reading off a queue
In order to manually test reading off the queue, the following command
can be run (would need to stop the relevant container to prevent it picking up
the event first).
```bash
aws --endpoint-url=http://localhost:4576 sqs receive-message --queue-url http://localhost:4576/queue/outbound_deletion_queue
```
