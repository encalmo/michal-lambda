# scala-aws-lambda-example

Simple example of the AWS Lambda function built on top of the `scala-aws-lambda-runtime`.

## Usage

Build native package using GraalVM.

```
./build.sh
```

The result will be:

- `/bin/bootstrap` binary file
- `/bin/function.zip` archive file. 

```
./run.sh
```

### More commands

Run lambda locally from source using command line interface:
```
./scripts/runLambdaLocallyUsingCommandLine.sh
```

Run lambda locally from source using Web Browser:
```
./scripts/runLambdaLocallyUsingWebBrowser.sh
```

Run native binary package locally:
```
./scripts/buildLambdaNativePackage.sh
```

Build and run native binary package locally:
```
./scripts/buildAndRunLambdaNativePackage.sh
```