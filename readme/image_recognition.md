[< Back](../README.md)
---

## Image Recognition

Used to help ensure there are no duplicates in the system before deletion. We use AWS Rekognition to find image matches.

### Creating an AWS Rekognition collection:

```
aws rekognition create-collection --collection-id "dps-offender-images"
```

### Setting up an IAM user to use the collection:

The permissions are added to the user as follows:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "rekognition:IndexFaces",
                "rekognition:DeleteFaces",
                "rekognition:SearchFaces",
                "rekognition:SearchFacesByImage",
                "rekognition:CompareFaces"
            ],
            "Resource": "<REKOGNITION RESOURCE>"
        },
        {
            "Effect": "Allow",
            "Action": "rekognition:CompareFaces",
            "Resource": "*"
        }
    ]
}
```

### Collection description:

The collection can be described with the following command:

```
aws rekognition describe-collection --collection-id dps-offender-images
```

## Configuring image upload

The image upload schedule can be configured using a cron expression.

*See below for an example:*

`IMAGE_RECOGNITION_MIGRATION_CRON: "0 0 13 ? * MON-FRI *"`

The number of images that are uploaded per second can also be configured. This is to prevent throttling by AWS
Rekognition. It defaults to 5.

*See below for an example:*

`IMAGE_RECOGNITION_UPLOAD_PERMITS_PER_SECOND: 14`

The number of threads allocated to the thread pool which is used to execute requests to retrieve Nomis images can be
configured. This defaults to 1.

*See below for an example:*

`PRISON_API_OFFENDER_IDS_ITERATION_THREADS: 2`
