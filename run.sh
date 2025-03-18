#!/bin/sh

# replace with yours
aws_profile=encalmo-sandbox

$(scala run --dependency=org.encalmo::setup-aws-credentials:0.9.2 --main-class=org.encalmo.aws.SetupAwsCredentials --quiet -- --profile $aws_profile)
echo "Using AWS profile $AWS_PROFILE, region $AWS_DEFAULT_REGION and access key $AWS_ACCESS_KEY_ID"

./scripts/buildAndRunLambdaNativePackage.sh
