package michal

import org.encalmo.aws.SetupAwsCredentials

trait TestSuite extends munit.FunSuite {

  val awsClientDebugMode = Map("AWS_CLIENT_DEBUG_MODE" -> "ON")

  val localAwsCredentials =
    SetupAwsCredentials(profile = "encalmo-sandbox")
      .map(_.toEnvironmentVariables)
      .getOrElse(Map.empty)
}
