[< Back](../README.md)
---
## Image Recognition

Used to help ensure there are no duplicates in the system before
deletion. We use AWS Rekognition to find image matches. 

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
                "rekognition:SearchFacesByImage"
            ],
            "Resource": "<REKOGNITION RESOURCE>"
        }
    ]
}
```

### Collection description:

The collection can be described with the following command:
```
aws rekognition describe-collection --collection-id jon-brighton-testing
```

