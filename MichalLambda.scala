package michal

import org.encalmo.lambda.LambdaContext
import org.encalmo.lambda.LambdaEnvironment
import org.encalmo.lambda.LambdaRuntime
import org.encalmo.aws.AwsClient
import org.encalmo.utils.JsonUtils.*
import upickle.default.*

import scala.annotation.static
import org.encalmo.aws.LambdaSecrets
import scala.language.experimental.namedTuples

object MichalLambda {

  @static def main(args: Array[String]): Unit = new MichalLambda().run()

  case class Config(greeting: String) derives ReadWriter
  case class Response(message: String) derives ReadWriter
}

class MichalLambda(maybeAwsClient: Option[AwsClient] = None) extends LambdaRuntime {

  import MichalLambda.*

  type ApplicationContext = (config: Config, awsClient: AwsClient)

  override def initialize(using environment: LambdaEnvironment): ApplicationContext = {
    val awsClient = maybeAwsClient
      .getOrElse(AwsClient.initializeWithProperties(environment.maybeGetProperty))

    val secrets = LambdaSecrets.retrieveSecrets(environment.maybeGetProperty)

    val greeting = secrets
      .get("SECRET_LAMBDA_GREETING")
      .getOrElse("Hello <input>!")

    environment.info(
      s"Initializing ${environment.getFunctionName()} with a greeting $greeting"
    )

    val config = Config(greeting)

    (config, awsClient)
  }

  override inline def handleRequest(
      input: String
  )(using lambdaConfig: LambdaContext, context: ApplicationContext): String = {
    val greeting = context.config.greeting.replace("<input>", input)
    println(s"Sending greeting: $greeting")
    Response(greeting).writeAsString
  }

}
