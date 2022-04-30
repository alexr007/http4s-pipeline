package org.alexr

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Ref
import cats.implicits.toFunctorOps
import io.circe.Json
import java.util.UUID
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import scala.util.chaining.scalaUtilChainingOps

object HttpServer extends IOApp.Simple with Http4sDsl[IO] {

  def doTheBusiness(n: Int) = n + 1

  /** http://localhost:8080/hello/Jim
    * http://localhost:8080/pipeline/100
    */
  def routes(ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name           => Ok(s"Hello: $name!")
    case GET -> Root / "pipeline" / IntVar(num) => doTheBusiness(num).pipe(x => Ok(x.toString))
  }

  def mkServer(ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) =
    BlazeServerBuilder[IO]
      .withHttpApp(routes(ref).orNotFound)
      .serve
      .void

  def mkPublisher(ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) = ???

  def mkConsumer(ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) = ???

  val app = for {
    ref <- IO.ref(Map.empty[UUID, Deferred[IO, Json]])
    server = mkServer(ref)
  } yield server.compile.drain

  override def run: IO[Unit] = app.flatten
}
