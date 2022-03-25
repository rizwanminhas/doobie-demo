import cats.effect.{ExitCode, IO, IOApp}


object DoobieDemo extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    IO(println("hello, doobie")).as(ExitCode.Success)
  }
}
