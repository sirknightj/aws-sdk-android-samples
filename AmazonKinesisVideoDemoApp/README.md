# Running AmazonKinesisVideoStreaming Sample

## 1. Provision and setup

Follow the prerequisites section to provision AWS resources required to run this sample, and assign the appropriate permissions the IAM role: [https://docs.aws.amazon.com/kinesisvideostreams/latest/dg/producer-sdk-android.html](https://docs.aws.amazon.com/kinesisvideostreams/latest/dg/producer-sdk-android.html)

* Amazon Cognito user pool
* Amazon Cognito identity pool

## 2. Paste
  * You will need all the information from the step above that you have :clipboard: and paste them into this file on your local copy of [awsconfiguration.json](src/main/res/raw/awsconfiguration.json). Here's what it should look like when you're done:
```json
{
  "Version": "1.0",
  "CredentialsProvider": {
    "CognitoIdentity": {
      "Default": {
        "PoolId": "us-west-2:01234567-89ab-cdef-0123-456789abcdef",
        "Region": "us-west-2"
      }
    }
  },
  "IdentityManager": {
    "Default": {}
  },
  "CognitoUserPool": {
    "Default": {
      "AppClientSecret": "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmno",
      "AppClientId": "0123456789abcdefghijklmnop",
      "PoolId": "us-west-2_qRsTuVwXy",
      "Region": "us-west-2"
    }
  }
}
```
  * Change the region that the app will stream to by editing the `KINESIS_VIDEO_REGION` constant in your local copy of [KinesisVideoDemoApp.java](https://github.com/awslabs/aws-sdk-android-samples/blob/master/AmazonKinesisVideoDemoApp/src/main/java/com/amazonaws/kinesisvideo/demoapp/KinesisVideoDemoApp.java)
