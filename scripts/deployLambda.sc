#!/usr/bin/env -S scala-cli shebang

//----------------------------------------
// Deploys lambda package
//----------------------------------------

//> using scala 3.6.3
//> using jvm 21
//> using toolkit 0.7.0
//> using dep org.encalmo::script-utils:0.9.0
//> using dep "org.encalmo::scala-aws-client:0.9.2,exclude=software.amazon.awssdk%apache-client,exclude=software.amazon.awssdk%netty-nio-client"
//> using dep org.encalmo::scala-aws-lambda-utils:0.9.2

import scala.io.AnsiColor.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.core.SdkBytes
import java.security.MessageDigest
import java.util.Base64
import java.nio.charset.StandardCharsets
import org.encalmo.utils.CommandLineUtils.*
import org.encalmo.utils.ConsoleUtils.*
import org.encalmo.aws.*
import org.encalmo.lambda.OptionPickler.*

case class Config(
    functionName: String,
    architecture: String,
    s3Bucket: Option[String] = None,
    tags: Map[String, String] = Map.empty
) derives upickle.default.ReadWriter

val lambdaDeploymentConfigPath = os.pwd / "lambda-deployment-config.json"
val zipFilePath = os.pwd / "bin" / "function.zip"

val description: String =
  optionalScriptParameter('d', "description")(args).getOrElse("")

if (!os.exists(lambdaDeploymentConfigPath)) {
  println(
    s"${RED}[ERROR] Path $lambdaDeploymentConfigPath does not exist in the current working directory.${RESET}"
  )
  System.exit(2)
}

if (!os.isFile(lambdaDeploymentConfigPath)) {
  println(s"${RED}[ERROR] Path $lambdaDeploymentConfigPath is not a file.${RESET}")
  System.exit(2)
}

if (!os.exists(zipFilePath)) {
  println(
    s"${RED}[ERROR] Path $zipFilePath does not exist in the current working directory.${RESET}"
  )
  System.exit(2)
}

if (!os.isFile(zipFilePath)) {
  println(s"${RED}[ERROR] Path $zipFilePath is not a file.${RESET}")
  System.exit(2)
}

val config: Config =
  upickle.default.read[Config](os.read(lambdaDeploymentConfigPath))

val functionName = config.functionName

val functionBytes: Array[Byte] = os.read.bytes(zipFilePath)
val objectVersion: String = MessageDigest
  .getInstance("SHA-256")
  .digest(functionBytes)
  .map("%02x".format(_))
  .mkString

val checksumSHA256: String = new String(
  Base64
    .getEncoder()
    .encode(
      MessageDigest
        .getInstance("SHA-256")
        .digest(functionBytes)
    )
)

given AwsClient = AwsClient.initialize()

val accountId: String = AwsStsApi.getCallerAccountId()

printlnMessageBoxed(
  color = GREEN,
  frame = '=',
  message =
    s"About to deploy lambda ${YELLOW}$functionName${GREEN} to the account ${YELLOW}$accountId${GREEN} with description \"${YELLOW}$description${GREEN}\""
)

println(
  s"${GREEN}$functionName package size is ${YELLOW}${functionBytes.length}${GREEN} bytes${RESET}"
)

println(s"${GREEN}Getting existing function configuration ... ${RESET}")

val function = AwsLambdaApi.getFunctionConfiguration(functionName)

println(s"${BLUE}${function}${RESET}")

if (function.codeSha256() == checksumSHA256) then {
  println(
    s"${GREEN}Current function already got that code with checksum ${YELLOW}${checksumSHA256}${RESET}, nothing to do here."
  )
} else {

  val (revisionId, codeSha256) = if (config.s3Bucket.isDefined) then {

    val nativeLambdaDeploymentPackagesBucket = config.s3Bucket.get

    val deploymentPackageObjectKey = s"${functionName}-${objectVersion}"

    println(
      s"${GREEN}Uploading new package to S3 bucket ${YELLOW}${nativeLambdaDeploymentPackagesBucket}${RESET}"
    )

    val isExistingPackage: Boolean = {
      if (
        AwsS3Api.checkObjectNotExists(
          nativeLambdaDeploymentPackagesBucket,
          deploymentPackageObjectKey
        )
      ) then {
        println(
          s"${GREEN}Package ${YELLOW}${deploymentPackageObjectKey}${GREEN} does not exist, uploading a new one with checksum ${YELLOW}$checksumSHA256${RESET}"
        )
        val response = AwsS3Api.putObjectUsingByteArray(
          nativeLambdaDeploymentPackagesBucket,
          deploymentPackageObjectKey,
          functionBytes,
          Map(
            "functionName" -> functionName,
            "description" -> description,
            "architecture" -> config.architecture,
            "checksumSHA256" -> checksumSHA256
          )
        )
        println(s"${GREEN}Uploaded file checksum is ${YELLOW}${response
            .checksumSHA256()}${RESET}")
        false
      } else {
        println(
          s"${GREEN}Package ${YELLOW}${deploymentPackageObjectKey}${GREEN} already exists in the S3 bucket, skipping upload${RESET}"
        )
        true
      }
    }

    AwsLambdaApi.updateFunctionCodeUsingS3Object(
      lambdaArn = function.functionArn(),
      architecture = config.architecture,
      bucketName = nativeLambdaDeploymentPackagesBucket,
      objectKey = deploymentPackageObjectKey,
      publish = false
    )
  } else {
    println(s"${GREEN}Uploading new package ... ${RESET}")
    AwsLambdaApi.updateFunctionCode(
      lambdaArn = function.functionArn(),
      architecture = config.architecture,
      zipFile = SdkBytes.fromByteArray(functionBytes),
      publish = false
    )
  }
  if (codeSha256 == checksumSHA256) then
    println(
      s"${GREEN}Function code upload done, revisionId is ${YELLOW}$revisionId${RESET}${GREEN}, checksum is ${YELLOW}$codeSha256${RESET}"
    )
  else {
    println(
      s"${RED_B}${WHITE}[FATAL] Function code upload succeeded but the uploaded file checksum ${YELLOW}$codeSha256${RESET}${RED_B}${WHITE} is different than the source file checksum ${YELLOW}$checksumSHA256${RESET}${RED_B}${WHITE}. Please investigate!!! ${RESET}"
    )
    System.exit(2)
  }

  val originalVariables: Map[String, String] =
    AwsLambdaApi.getFunctionEnvironmentVariables(function.functionArn())

  val aliasPlaceholder = "${alias}"

  val variablesHaveAliasPlaceholder: Boolean =
    originalVariables.exists((k, v) => v.contains(aliasPlaceholder))

  val version =
    if (variablesHaveAliasPlaceholder)
    then {
      println(
        s"${GREEN}This function's environment variables are parametrized with alias placeholder${RESET}"
      )
      println(
        s"${GREEN}because of that we will create and deploy separate versions for each alias${RESET}"
      )
      ""
    } else {
      println(
        s"${GREEN}Publishing new version ... ${RESET}"
      )
      val version = AwsLambdaApi.publishNewVersion(function.functionArn(), description)
      println(
        s"${GREEN}Published new version ${YELLOW}$version${RESET}"
      )
      version
    }

  println(s"${GREEN}Listing function aliases ... ${RESET}")
  val aliases = AwsLambdaApi.getFunctionAliases(function.functionArn())

  if (aliases.isEmpty)
  then {
    if (variablesHaveAliasPlaceholder)
    then {
      println(
        s"${WHITE}${RED_B}[ERROR] Function got no aliases defined but environment variables have alias placeholder${RESET}"
      )
      System.exit(2)
    } else {
      println(
        s"${GREEN}Function got no aliases defined, $$LATEST version is now ${YELLOW}${version}${RESET}"
      )
    }
  } else {

    aliases.foreach { alias =>
      try {
        println(
          s"${GREEN}Publishing to alias ${YELLOW}${alias.name()}${GREEN} ...${RESET}"
        )

        if (variablesHaveAliasPlaceholder) {
          val modifiedVariables: Map[String, String] =
            originalVariables
              .mapValues(value => value.replace(aliasPlaceholder, alias.name()))
              .toMap

          println(
            s"${GREEN}Updating environment variables ... ${RESET}"
          )
          val revisionId2 = AwsLambdaApi.updateFunctionEnvironmentVariables(
            function.functionArn(),
            modifiedVariables
          )
          println(
            s"${GREEN}Updated environment variables for ${alias.name()} version${RESET}"
          )

          println(
            s"${GREEN}Publishing new version ... ${RESET}"
          )
          val version = AwsLambdaApi.publishNewVersion(
            function.functionArn(),
            s"[${alias.name()}] $description"
          )
          println(
            s"${GREEN}Published new version ${YELLOW}$version${RESET}"
          )

          println(
            s"${GREEN}Updating alias ... ${RESET}"
          )
          val aliasArn =
            AwsLambdaApi.updateAlias(function.functionArn(), alias.name(), version)
          println(
            s"${GREEN}Successfully updated alias ${YELLOW}${alias
                .name()}${GREEN} to version ${YELLOW}${version}${RESET}"
          )
        } else {
          println(
            s"${GREEN}Updating alias ... ${RESET}"
          )
          val aliasArn =
            AwsLambdaApi.updateAlias(function.functionArn(), alias.name(), version)
          println(
            s"${GREEN}Successfully updated alias ${YELLOW}${alias
                .name()}${GREEN} to version ${YELLOW}${version}${RESET}"
          )
        }
      } catch {
        case e => println(s"${WHITE}${RED_B}[ERROR] ${e}${RESET}")
      }
    }

    if (variablesHaveAliasPlaceholder) {
      println(
        s"${GREEN}Updating environment variables ... ${RESET}"
      )
      val revisionId2 = AwsLambdaApi.updateFunctionEnvironmentVariables(
        function.functionArn(),
        originalVariables
      )
      println(
        s"${GREEN}Updated environment variables back to the original values${RESET}"
      )
    }
  }
}

// if (config.tags.nonEmpty) {
//   AwsLambdaApi.tagLambda(function.functionName, tags)
// }

println(
  s"${GREEN}Done.${RESET}"
)
