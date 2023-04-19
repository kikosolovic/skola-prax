import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
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
      } yield Response(status, body)
  }
  Console.println("Search: ")
  val query = scala.io.StdIn.readLine()
  val runF = for {
    _ <- request(HttpMethods.GET, path = "/search", params = Some(s"query=$query")).exec.map(println)
//    _ <- request(HttpMethods.GET).exec.map(println)
//    _ <- request(HttpMethods.GET, path = "/hello").exec.map(println)
//    _ <- request(HttpMethods.GET, path = "/hello_to", params = Some("nameeee=Zoltan")).exec.map(println)
//    _ <- request(HttpMethods.GET, path = "/search", params = Some("query=Zoltan")).exec.map(println)
//    _ <- request(HttpMethods.GET, path = "/user/1234").exec.map(println)
//    _ <- request(
//      HttpMethods.POST,
//      path = "/user/1234/delete",
//      body = Some(HttpEntity(ContentTypes.`application/json`, "{\"key\": \"value\"}"))
//    ).exec.map(println)
  } yield ()
  runF.andThen(_ => as.terminate())
}
