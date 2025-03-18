#!/bin/sh

echo "Compiling project ..."

scala-cli version
scala-cli compile .
# scala-cli test .

echo "Preparing native image ..."
scala --power package --native-image . -o ./bin/bootstrap \
    --graalvm-jvm-id="graalvm-java21:21.0.2" \
    --force -- \
    --verbose \
    --no-fallback \
    --enable-http \
    --enable-https \
    --strict-image-heap \
    -H:+UnlockExperimentalVMOptions \
    -H:ReflectionConfigurationFiles=reflect.json \
    -H:ClassInitialization=org.slf4j:build_time \
    -H:+ReportUnsupportedElementsAtRuntime \
    -R:MaxHeapSize=64m \
    -Dsoftware.amazon.awssdk.http.service.impl=software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService

cd bin
chmod 755 bootstrap
zip function.zip bootstrap

ls -l function.zip

echo "Done."
