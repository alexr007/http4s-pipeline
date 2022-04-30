package org.alexr

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import fs2.kafka.KafkaProducer

object ProducerApp extends IOApp.Simple {

  import KSyntax._

  val resources: Resource[IO, KafkaProducer[IO, String, String]] =
    KConfig.producerSettings[IO, String, String]

  val app = resources.use { k =>
    k.produceOne("in", "K5", "V5")
  }

  override def run: IO[Unit] = app.void
}
