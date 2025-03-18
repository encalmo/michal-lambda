#!/bin/sh

if [ -f "bin/bootstrap" ]; then
    ./scripts/runLambdaNativePackageLocally.sh
else
    ./scripts/buildLambdaNativePackage.sh
    ./scripts/runLambdaNativePackageLocally.sh
fi
