import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats.implicits._
import doobie.{HC, HPS}
import fs2.Stream

object DoobieDemo extends IOApp {

  implicit class Debugger[A](io: IO[A]) {
    def debug: IO[A] = io.map { a =>
      println(s"[${Thread.currentThread().getName}] $a")
      a
    }
  }

  case class Actor(id: Int, name: String)
  case class Movie(id: String, title: String, year: Int, actors: List[String], director: String)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/myimdb",
    "docker",
    "docker")

  def findAllActorNames: IO[List[String]] = {
    val query = sql"select name from actors".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  def findActorById(id: Int): IO[Actor] = {
    val query = sql"select id, name from actors where id=$id".query[Actor]
    val action = query.unique
    action.transact(xa)
  }

  def findOptionalActorById(id: Int): IO[Option[Actor]] = {
    val query = sql"select id, name from actors where id=$id".query[Actor]
    val action = query.option
    action.transact(xa)
  }

  val actorNamesStream = sql"select name from actors".query[String].stream.compile.toList.transact(xa)

  def printActorsInBatch(batchSize: Int): Unit = {
    //sql"select id, name from actors".query[Actor].stream.take(batchSize).compile.toList.transact(xa).unsafeRunSync().foreach(println)

    sql"select id, name from actors"
      .query[Actor]
      .stream
      .take(batchSize)
      .compile
      .toList
      .transact(xa)
      .unsafeRunSync()
      .foreach(println)
  }

  // HC, HPS
  def findActorByName(name: String): IO[Option[Actor]] = {
    val query = "select id, name from actors where name = ?"
    HC.stream[Actor](
      query,
      HPS.set(name),
      100
    ).compile.toList.map(_.headOption).transact(xa)
  }

  // fragments

  def findActorByInitial(letter: String): IO[List[Actor]] = {
    val select = fr"select id, name"
    val fromPart = fr"from actors"
    val wherePart = fr"where LEFT(name, 1) = $letter"

    val statement = select ++ fromPart ++ wherePart
    statement.query[Actor].stream.compile.toList.transact(xa)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    //IO(println("hello, doobie")).as(ExitCode.Success)
    //findAllActorNames.debug.as(ExitCode.Success)
    //findActorById(1).debug.as(ExitCode.Success)
    //findOptionalActorById(100).debug.as(ExitCode.Success)
    //actorNamesStream.debug.as(ExitCode.Success)
    //(1 to 3).foreach(n => printActorsInBatch(2))
    //IO().as(ExitCode.Success)
    //findActorByName("Henry Cavill").debug.as(ExitCode.Success)
    findActorByInitial("H").debug.as(ExitCode.Success)
  }
}
