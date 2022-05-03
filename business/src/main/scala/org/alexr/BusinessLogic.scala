package org.alexr

import cats.effect.IO
import cats.effect.IOApp
import io.circe.generic.AutoDerivation
import java.util.concurrent.Executors
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.chaining.scalaUtilChainingOps

object BusinessLogic extends IOApp.Simple with Http4sDsl[IO] {

  val ecBlocking: ExecutionContext = Executors.newFixedThreadPool(1000).pipe(ExecutionContext.fromExecutorService)
  val ecHttp: ExecutionContext = Executors.newFixedThreadPool(32).pipe(ExecutionContext.fromExecutorService)

  case class In(value: Int)
  object In extends AutoDerivation
  case class Out(value: Int)
  object Out extends AutoDerivation
  def doBusiness(x: Int): Int = x + 1
  def business(x: Int) = IO.sleep(3.seconds).as(doBusiness(x))

  val routes = HttpRoutes.of[IO] {
    case GET -> Root        => Ok("This is business logic service")
    case GET -> Root / "1s" => IO.sleep(1.second) >> Ok("This is business logic service")

    case rq @ POST -> Root / "do_fast" =>
      rq.as[In].flatMap { in =>
        val outcome = doBusiness(in.value)
        val entity = Out(outcome)
        Ok(entity)
      }

    case rq @ POST -> Root / "do_slow" =>
      rq.as[In].flatMap { in =>
        business(in.value)
          .map(Out(_))
          .flatMap(Ok(_))
      }
  }

  val http = BlazeServerBuilder[IO]
    .bindHttp(8081)
//    .withExecutionContext(ecHttp)
    .withHttpApp(routes.orNotFound)
    .serve
    .compile
    .drain

  override def run: IO[Unit] = http
}
