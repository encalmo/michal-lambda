#!/bin/sh

# replace with yours
aws_profile=encalmo-sandbox

$(scala run --dependency=org.encalmo::setup-aws-credentials:0.9.2 --main-class=org.encalmo.aws.SetupAwsCredentials --quiet -- --profile $aws_profile)
echo "Using AWS profile $AWS_PROFILE, region $AWS_DEFAULT_REGION and access key $AWS_ACCESS_KEY_ID"

export MUNIT_FLAKY_OK=true

if [ $# -ge 1 ]; then
    SUFFIX="--test-only *$1"
else
    SUFFIX=''
fi
if scala test . $SUFFIX --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning --suppress-outdated-dependency-warning; then
    echo "Done."
else
    echo "Tests failed, check the log for the details."
fi
