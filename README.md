# DPS Data Compliance 

[![CircleCI](https://circleci.com/gh/ministryofjustice/dps-data-compliance.svg?style=svg)](https://circleci.com/gh/ministryofjustice/dps-data-compliance)

A Spring Boot app to manage data compliance for the Digital Prison Services

## Publishing Deletion Event to SNS Topic

Once the DPS Data Compliance Service identifies an offender for deletion, it adds 
an event onto the offender_events topic with an `eventType` of 
`DATA_COMPLIANCE_DELETE-OFFENDER`.

Other services such as Elite2 API pick up this event from an SNS queue and delete
the offender data as a result.

The following instructions show how to get DPS Data Compliance, Elite2 API and 
Localstack up and running in Docker.

### Running localstack
```bash
TMPDIR=/private$TMPDIR docker-compose up localstack
```

### Creating the Topic and Queue
Simpliest way is running the following script
```bash
./setup-sns.bash
```

Or you can run the scripts individually as shown below.

### Creating a topic and queue on localstack

```bash
aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events
```

Results in:
```json
{
    "TopicArn": "arn:aws:sns:eu-west-2:000000000000:offender_events"
}

```

### Creating a queue
```bash
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name elite2_api_queue
```

Results in:
```json
{
   "QueueUrl": "http://localhost:4576/queue/event_queue"
}
```

### Creating a subscription
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

### Read off the queue
```bash
aws --endpoint-url=http://localhost:4576 sqs receive-message --queue-url http://localhost:4576/queue/elite2_api_queue
```

### Then running dps-data-compliance and elite2-api 
```bash
docker-compose up dps-data-compliance elite2-api 
```
