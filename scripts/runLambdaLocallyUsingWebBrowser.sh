#!/bin/sh

scala run --dependency=org.encalmo::scala-aws-lambda-local-host:0.9.1 \
    --main-class org.encalmo.lambda.host.LocalLambdaHost \
    --quiet --suppress-directives-in-multiple-files-warning \
    --suppress-outdated-dependency-warning \
    -- \
    --mode=browser \
    --lambda-script="scala run --main-class michal.MichalLambda --quiet --suppress-directives-in-multiple-files-warning --suppress-outdated-dependency-warning ." \
    --lambda-name=MichalLambda
