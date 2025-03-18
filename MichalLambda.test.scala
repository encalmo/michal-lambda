package michal

import org.encalmo.utils.JsonUtils.*

class MichalLambda2Spec extends TestSuite {

/*  test("MichalLambda should respond with a message containing a configured greeting") {

    val environment =
      localAwsCredentials
        ++ Map(
          "ENVIRONMENT_SECRETS" -> "arn:aws:secretsmanager:eu-central-1:047719648492:secret:test-secret-rJbTad"
        )

    val input = "Michał"

    val output = new MichalLambda().test(input, environment)

    assertEquals(output, "{\"message\":\"Hello Michał! Welcome to the Wrocław Scala User Group (WSUG) meetup!\"}")
  }

  test("MichalLambda should respond with a default greeting") {

    val environment = localAwsCredentials

    val response = new MichalLambda()
      .test("Scala", environment)
      .readAs[MichalLambda.Response]

    assertEquals(response.message, "Hello Scala!")
  }*/

}
