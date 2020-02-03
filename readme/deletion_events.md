[< Back](../README.md)
---
## Publishing Deletion Event to SNS Topic

Once the DPS Data Compliance Service identifies an offender for deletion, it adds 
an event onto the offender_events topic with an `eventType` of 
`DATA_COMPLIANCE_DELETE-OFFENDER`.

Other services such as Elite2 API pick up this event from an SNS queue and delete
the offender data as a result.

### Setup

To demonstrate locally how the event publishing and subscription
works, we can set up services and a Localstack instance using docker:

```bash
TMPDIR=/private$TMPDIR docker-compose up
```

### Topic and Queue
The localstack section in docker-compose.yml ensures that the files
`localstack/setup-sns.sh` and `localstack/set-queue-attributes.json`
are copied into an auto-execution directory in the container.

The `setup-sns.sh` script ensures the topic and queue are created by
using the following AWS CLI commands:

#### Topic:
```bash
aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events
```

Results in:
```json
{
    "TopicArn": "arn:aws:sns:eu-west-2:000000000000:offender_events"
}

```

#### Queue
```bash
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name elite2_api_queue
```

Results in:
```json
{
   "QueueUrl": "http://localhost:4576/queue/event_queue"
}
```

#### Subscription
```bash
aws --endpoint-url=http://localhost:4575 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4576/queue/elite2_api_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"DATA_COMPLIANCE_DELETE-OFFENDER\"]}"}'
```

Results in:
```json
{
    "SubscriptionArn": "arn:aws:sns:eu-west-2:000000000000:offender_events:074545bd-393c-4a43-ad62-95b1809534f0"
}
```

### Publishing to the queue
(Warning, if configured, this will prompt Elite2 to delete the Offender provided)

The following command can be run after the docker containers have started up
in order to simulate a deletion (replacing `SOME_OFFENDER_ID_DISPLAY` with a
valid offender number:

```bash
aws --endpoint-url=http://localhost:4575 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --message '{"offenderIdDisplay":"SOME_OFFENDER_ID_DISPLAY"}' \ 
    --message-attributes "eventType={StringValue=DATA_COMPLIANCE_DELETE-OFFENDER,DataType=String}"
```

### Reading off the queue
In order to manually test reading off the queue, the following command
can be run (would need to stop the Elite2 API container to prevent it picking up
the event first).
```bash
aws --endpoint-url=http://localhost:4576 sqs receive-message --queue-url http://localhost:4576/queue/elite2_api_queue
```
