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
import org.encalmo.lambda.{ApiGatewayRequest, ApiGatewayResponse}
import Part1.* 


object MichalLambda {

  @static def main(args: Array[String]): Unit = new MichalLambda().run()

  
  case class Response(message: String) derives ReadWriter
}

class MichalLambda(maybeAwsClient: Option[AwsClient] = None) extends LambdaRuntime {

  import MichalLambda.*


  //definition of application
  type ApplicationContext = AwsClient

  override def initialize(using environment: LambdaEnvironment): ApplicationContext = {
    val awsClient = maybeAwsClient
      .getOrElse(AwsClient.initializeWithProperties(environment.maybeGetProperty))    

    awsClient
  }

  override inline def handleRequest(
      input: String
  )(using lambdaConfig: LambdaContext, context: ApplicationContext): String = 
    input
      .maybeReadAs[ApiGatewayRequest]
     .map(request => 
      ApiGatewayResponse(
      Part1.solve(request.body),
      statusCode = 200,
      headers = Map(
        "Content-Type"->"application/json"
      ),
      isBase64Encoded = false
        )
    ).getOrElse{
      ApiGatewayResponse(
        body = "Cannot parse input",
        statusCode = 400,
        headers = Map.empty,
        isBase64Encoded = false
      )
    }.writeAsString
    
    

}
