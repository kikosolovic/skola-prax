import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import scala.io.StdIn
import akka.http.scaladsl.model.headers.RawHeader
import scala.util.{Failure, Success}
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.duration.Duration
import spray.json._
import DefaultJsonProtocol._

case class Items(originalImageUrl: String, title:String)
case class Response(status: String, items: List[Items])

object ResponseJsonProtocol extends DefaultJsonProtocol {
  implicit val itemsFormat: RootJsonFormat[Items] = jsonFormat2(Items)
  implicit val responseFormat: RootJsonFormat[Response] = jsonFormat2(Response)
}

object server extends App {
  import ResponseJsonProtocol._
  import com.spse.QueryDBSetup
  implicit val actorSystem = ActorSystem()
  implicit val ec = actorSystem.dispatcher

  implicit class MapToJson[V](params: Map[String, V]) {
    def toUrlParams: String = params.map { case (k, v) => s"$k=$v" }.mkString("&")
  }


  val route = concat(
    path("imagesearch") {
      get {
        parameters("query".as[String]) { query =>
          val requestParams = Map(
            "query" -> query,
            "sort" -> "relevance",
            "num" -> "1"
          ).toUrlParams

          val request = HttpRequest(
            method = HttpMethods.GET,
            uri = s"https://google-search72.p.rapidapi.com/imagesearch?$requestParams",
            headers = Seq(
              RawHeader("X-RapidAPI-Key", "047da99f91msh1522bafa8d4de44p110a3fjsnf98ee48980b7"),
              RawHeader("X-RapidAPI-Host", "google-search72.p.rapidapi.com")
            ),
          )

          val performRequestFut = for {
            response <- Http().singleRequest(request)
            body <- Unmarshal(response.entity).to[String]
          } yield body

          onComplete(performRequestFut) {
            case Success(result) =>
              println("doslo sem,")
//              tu to skonci neviempreco
              val response : Response = result.parseJson.convertTo[Response]
              QueryDBSetup.ServerInsert(query,response.items.head.originalImageUrl )
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Look at <${response.items.head.title}> image at this link   <${response.items.head.originalImageUrl}>"))
            case Failure(exception) =>
              println("mega gay")
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "supergay"))

          }
        }
      }
    },
    pathEndOrSingleSlash {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Response from server"))
      }
    }
  )

  val bindingFut = Http().newServerAt("localhost", 8080).bind(route)
  bindingFut.onComplete {
    case Success(binding) =>
      println(s"Server running on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}")
    case Failure(ex) =>
      println(s"Failed to start the server: ${ex.getMessage}")
      actorSystem.terminate()
  }

  StdIn.readLine()
  bindingFut.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
}
