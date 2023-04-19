import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import scala.io.StdIn
import akka.http.scaladsl.model.headers.RawHeader
import scala.util.{Failure, Success}
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.{ Await }
import scala.concurrent.duration.Duration


object server extends App {
  implicit val actorSystem = ActorSystem()
  implicit val ec = actorSystem.dispatcher

  implicit class MapToJson[V](params: Map[String, V]) {
    def toUrlParams: String = params.map { case (k, v) => s"$k=$v" }.mkString("&")
  }


  val route = {
    concat(

      path("search"){
        get {
          parameters("query".as[String]) {query =>
//            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Results for $query"))
            val requestParams = Map(
              "query" -> s"$query",
              "sort" -> "relevance",

              "num" -> "1",

            ).toUrlParams
            val request = HttpRequest(
              method = HttpMethods.GET,
              uri = s"https://google-search72.p.rapidapi.com/imagesearch?$requestParams",
              headers = Seq(
                RawHeader("X-RapidAPI-Key", "0b031cc79bmshe8e881b4ca3d931p1d4e40jsn4142da721e80"),
                RawHeader("X-RapidAPI-Host", "google-search72.p.rapidapi.com")
              ))

            val performRequestFut = for {
                response <- Http().singleRequest(request)
                body <- Unmarshal(response.entity).to[String]
                _ = response.entity.discardBytes()
              } yield (body)
//              println(s"status: $status, body: $body")
            Await.ready(performRequestFut, Duration.Inf)
//            val res = performRequestFut[2]
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Results for ${performRequestFut.value}"))
//            performRequestFut.onComplete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Results for $performRequestFut.")   )




//
          }
        }
      },





      path("hello_to") {
        get {
          parameters("name".as[String]) { name =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Hello to $name"))
          }
        }
      },

      path("hello") {
        get {
          onComplete(Future {
            "ourString"
          }) {
            case Success(value) => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Hello to everyone"))
            case Failure(exception) => complete(InternalServerError, s"An error occurred: ${exception.getMessage}")
          }
        }
      },
      pathEndOrSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Response from server"))
        }
      },
      pathPrefix("user" / LongNumber)( userId => concat(
        pathEndOrSingleSlash {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"user detail of user $userId"))
          }
        },
        path("delete") {
          decodeRequest {
            post {
              entity(as[String]) { ent: String =>
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"deleting user $userId with params $ent"))
              }
            }
          }
        }
      ))
    )
  }
  val bindingFut = for {
    binding <- Http().newServerAt("localhost", 8080).bind(route)
    _ = println(s"Server running on ${binding.localAddress.getHostName}:${binding.localAddress.getPort}")
  } yield binding
  StdIn.readLine()
  bindingFut.flatMap(_.unbind()).andThen(_ => actorSystem.terminate())
}
