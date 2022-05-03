package org.alexr

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Ref
import cats.effect.Resource
import fs2.kafka.KafkaConsumer
import fs2.kafka.KafkaProducer
import io.circe.Json
import io.circe.syntax.EncoderOps
import java.util.UUID
import org.alexr.topics.Input
import org.alexr.topics.Input.KInputPayload
import org.alexr.topics.Output
import org.alexr.topics.Output.KOutputPayload
import org.http4s.EntityEncoder.Pure
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl

object HttpServer extends IOApp.Simple with Http4sDsl[IO] {

  /** http://localhost:8080/hello/Jim
    * http://localhost:8080/pipeline/301
    */
  def routes(publisher: KafkaProducer[IO, String, String], ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name           => Ok(s"Hello: $name!")
    case GET -> Root / "pipeline" / IntVar(num) =>
      /** identifier */
      val id = UUID.randomUUID()
      val kKey = id.asJson.noSpaces

      /** payload */
      val kValue = KInputPayload(num).asJson.noSpaces

      import KSyntax._
      implicit val ee: Pure[Json] = jsonEncoderOf[Json]

      for {
        /** create hook */
        d       <- Deferred[IO, Json]
        /** register hook in the global storage */
        _       <- ref.update(m => m + (id -> d))
        /** send to kafka */
        _       <- publisher.produceOne(Input.name, kKey, kValue)
        /** wait for a completion from Kafka Consumer */
        outcome <- d.get
      } yield Response[IO]().withEntity(outcome)
  }

  val mkPublisher = KConfig.producer[IO, String, String]

  def mkServer(publisher: KafkaProducer[IO, String, String], ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) =
    BlazeServerBuilder[IO]
      .withHttpApp(routes(publisher, ref).orNotFound)
      .resource

  val consumerSettings = KConfig.consumerSettings[IO, String, String]("originator")

  def mkConsumer(ref: Ref[IO, Map[UUID, Deferred[IO, Json]]]) = KafkaConsumer
    .stream(consumerSettings)
    .evalTap(_.subscribeTo(Output.name))
    .flatMap(_.stream)
    .evalMap { ccr =>
      val keyRaw: String = ccr.record.key
      println(keyRaw)
      val key: UUID = UUID.fromString(keyRaw.stripPrefix("\"").stripSuffix("\""))
      val valueRaw = ccr.record.value
      val value: KOutputPayload = io.circe.parser.decode[KOutputPayload](valueRaw).getOrElse(???)
      ref
        .modify { map =>
          val d: Deferred[IO, Json] = map(key)
          val map2 = map - key
          (map2, d)
        }
        .flatMap(_.complete(value.asJson)) >>
        ccr.offset.commit
    }
    .compile
    .resource
    .drain

  val app = for {
    ref       <- Resource.eval(IO.ref(Map.empty[UUID, Deferred[IO, Json]]))
    publisher <- mkPublisher
    server    <- mkServer(publisher, ref)
    consumer  <- mkConsumer(ref)
  } yield (server, consumer)

  override def run: IO[Unit] = app.use(_ => IO.never)
}
