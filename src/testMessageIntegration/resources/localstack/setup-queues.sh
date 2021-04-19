#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=anykey
export AWS_SECRET_ACCESS_KEY=anysecret
export AWS_DEFAULT_REGION=eu-west-2

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name data_compliance_response_queue
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name data_compliance_response_dead_letter_queue
aws --endpoint-url=http://localhost:4576 sqs set-queue-attributes \
    --queue-url http://localhost:4576/queue/data_compliance_response_queue --attributes '{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:data_compliance_response_dead_letter_queue\",\"maxReceiveCount\":\"1\"}"}'

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name data_compliance_request_queue
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name data_compliance_request_dead_letter_queue
aws --endpoint-url=http://localhost:4576 sqs set-queue-attributes \
    --queue-url http://localhost:4576/queue/data_compliance_request_queue --attributes '{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:data_compliance_request_dead_letter_queue\",\"maxReceiveCount\":\"1\"}"}'

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name delete_offender_queue
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name delete_offender_dead_letter_queue
aws --endpoint-url=http://localhost:4576 sqs set-queue-attributes \
    --queue-url http://localhost:4576/queue/delete_offender_queue --attributes '{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:delete_offender_dead_letter_queue\",\"maxReceiveCount\":\"1\"}"}'

aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events
aws --endpoint-url=http://localhost:4575 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4576/queue/delete_offender_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"DATA_COMPLIANCE_DELETE-OFFENDER\"]}"}'


echo All Ready