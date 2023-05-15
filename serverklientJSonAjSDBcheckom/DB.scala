package com.spse

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
// sqlite jdbc jar in lib (https://github.com/xerial/sqlite-jdbc/releases/tag/3.41.2.1), install sqlite
class SQLiteConnector(private val path: String, private val workerCount: Int) {
  private class Worker {
    private val connection: Connection = DriverManager.getConnection(s"jdbc:sqlite:$path")
    private val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))
    private val isReady: AtomicBoolean = new AtomicBoolean(true)
    def doWork[R](task: Connection => R): Option[Future[R]] =
      if (isReady.getAndSet(false))
        Some(Future(task(connection))(ec).andThen {case _ => isReady.set(true)}(ec))
      else
        None
    def terminate(): Future[Unit] = Future {
      isReady.set(false)
      connection.close()
      ec.shutdown()
    }(ec) // as we have only 1 thread, this will execute after any task running
  }
  private val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))
  private val workers: List[Worker] = 0.until(workerCount).map(_ => new Worker).toList
  private def execute[R](execFn: Statement => R): Future[R] = Future(blocking {
    def tryRun(workersToTry: List[Worker] = workers): Option[Future[R]] = workersToTry match {
      case Nil =>
        Thread.sleep(500)
        tryRun(workers)
      case current :: rest =>
        current.doWork(connection => execFn(connection.createStatement())).orElse(tryRun(rest))
    }
    tryRun().get
  })(ec).flatten
  def executeStmt(stmt: String): Future[Boolean] = execute(_.execute(stmt))
  def query[R](q: String, resultExtractFn: ResultSet => R): Future[List[R]] = {
    @tailrec
    def unfoldResultSet(resultSet: ResultSet, res: List[R] = List.empty): List[R] =
      if (resultSet.next())
        unfoldResultSet(resultSet, resultExtractFn(resultSet) :: res)
      else
        res
    execute(_.executeQuery(q)).map(resultSet => unfoldResultSet(resultSet).reverse)(ec)
  }
  def close(): Future[Unit] = {
    implicit val iec: ExecutionContext = ec
    Future.sequence(workers.map(_.terminate())).map(_ => ec.shutdownNow())
  }
}
object SQLiteConnector {
  def apply(path: String, workerCount: Int): Try[SQLiteConnector] = Try(new SQLiteConnector(path, workerCount))
}


object QueryDBSetup extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  SQLiteConnector("/Users/kisol/Desktop/SQLite/data.sqlite", 5).map { db =>
    val doQueries = for {
      _ <- db.executeStmt(
        """
          |CREATE TABLE IF NOT EXISTS queries (
          | id integer PRIMARY KEY,
          | query text NOT NULL,
          | result text NOT NULL
          |)
          |""".stripMargin
      )
    } yield ()
    doQueries.recover { case e => e.printStackTrace() }
    Await.ready(doQueries, Duration.Inf)
    Await.ready(db.close().recover { case e => e.printStackTrace() }, Duration.Inf)
  }
    .recover { case e => e.printStackTrace() }

  def QueryCheck(search: String): String = {
    val url = "jdbc:sqlite:/Users/kisol/Desktop/SQLite/data.sqlite"
    val connection: Connection = DriverManager.getConnection(url)
    val a = connection.prepareStatement(
      s"""
         |SELECT result FROM queries WHERE query = '$search'
         |""".stripMargin)


    val resultSet : ResultSet = a.executeQuery()
    if (resultSet.next()) {
      val result: String = resultSet.getString("result")
      connection.close()
      result
    } else {
      connection.close()
      null
    }

      }
  def ServerInsert(search : String, linkResult : String) : Unit ={
    val url = "jdbc:sqlite:/Users/kisol/Desktop/SQLite/data.sqlite"
    val connection: Connection = DriverManager.getConnection(url)
    val a = connection.prepareStatement(
      s"""
         |INSERT INTO queries(query,result) VALUES('$search','$linkResult')
         |""".stripMargin)
    a.execute()
    connection.close()
  }
}

object SimpleDb extends App {
  val connection: Connection = DriverManager.getConnection("jdbc:sqlite:/Users/kisol/Desktop/SQLite/data.sqlite")
  try {
    val result = connection.createStatement().executeQuery("SELECT * FROM persons")
    while (result.next()) println((result.getString(1), result.getString(2)))
  } finally {
    connection.close()
  }
}
