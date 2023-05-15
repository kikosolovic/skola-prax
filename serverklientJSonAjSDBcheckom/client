import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.spse.QueryDBSetup

import scala.concurrent.Future

object client extends App {
  implicit val as = ActorSystem()
  implicit val ec = as.dispatcher
  case class Response(status: Int, body: String)
  def request(method: HttpMethod, path: String = "", params: Option[String] = None, body: Option[RequestEntity] = None): HttpRequest =
    HttpRequest(
      method = method,
      uri = s"http://localhost:8080$path${params.map(p => s"?$p").getOrElse("")}",
      headers = Seq(),
      entity = body.getOrElse(HttpEntity.Empty)
    )
  implicit class UltraHttpRequest(request: HttpRequest) {
    def exec: Future[Response] =
      for {
        response <- Http().singleRequest(request)
        status = response.status.intValue()
        body <- Unmarshal(response.entity).to[String]
        _ = response.entity.discardBytes()
      } yield Response( status, body)
  }
  Console.println("Search for an image: ")
  val query = scala.io.StdIn.readLine()

QueryDBSetup.QueryCheck(query) match{

    case null =>

         val runF = for {
           _ <- request(HttpMethods.GET, path = "/imagesearch", params = Some(s"query=$query")).exec.map(println)
         } yield ()
         runF.andThen(_ => as.terminate())
    case value => println("z DB -> "+value)
  }


}



