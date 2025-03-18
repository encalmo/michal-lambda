#!/bin/sh

help() {
    echo "Usage: buildAndDeployLambda.sh heapsize changelog"
    exit 2
}

run() {

    export MUNIT_FLAKY_OK=true

    if scala-cli --power test .; then

        echo "Preparing native image ..."
        scala --power package --native-image . -o ./bin/bootstrap \
            --graalvm-jvm-id="graalvm-java21:21.0.2" \
            --force -- \
            --no-fallback \
            --enable-http \
            --enable-https \
            --strict-image-heap \
            -march=compatibility \
            -H:+UnlockExperimentalVMOptions \
            -H:ReflectionConfigurationFiles=reflect.json \
            -H:ClassInitialization=org.slf4j:build_time \
            -H:+ReportUnsupportedElementsAtRuntime \
            -R:MaxHeapSize=$heapsize \
            -Dsoftware.amazon.awssdk.http.service.impl=software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService

        if [ -d bin ] && [ -f bin/bootstrap ]; then

            cd bin
            chmod 755 bootstrap
            zip function.zip bootstrap

            ls -l function.zip

            cd ..

            scala --power ./scripts/deployLambda.sc \
                -- \
                --description="$changelog"

        else
            echo "\033[31mFailure: Missing ${folder}/bin/boostrap file. Check native-package build logs.\033[0m"
            exit 2
        fi
    else
        echo "Tests failed, check the log."
        exit 2
    fi
}

if [ $# -eq 2 ]; then

    heapsize=$1
    changelog=$2

    run
else
    help
fi
