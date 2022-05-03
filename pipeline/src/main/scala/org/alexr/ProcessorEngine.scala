package org.alexr

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import fs2.kafka.KafkaConsumer
import fs2.kafka.KafkaProducer
import io.circe.syntax.EncoderOps
import org.alexr.topics.Input
import org.alexr.topics.Input.KInputPayload
import org.alexr.topics.Output
import org.alexr.topics.Output.KOutputPayload
import scala.concurrent.duration.DurationInt
import scala.util.Random

object ProcessorEngine extends IOApp.Simple {

  def doTheBusiness(value: Int): Int = value + 1

  val publisher: Resource[IO, KafkaProducer[IO, String, String]] = KConfig.producer[IO, String, String]

  val consumerSettings = KConfig.consumerSettings[IO, String, String]("pipeline")

  val consumer = KafkaConsumer
    .stream(consumerSettings)
    .evalTap(_.subscribeTo(Input.name))
    .flatMap(_.stream)

  val random = new Random()

  def app(publisher: KafkaProducer[IO, String, String]) = consumer
    .parEvalMap(10) { ccr =>
      val key: String = ccr.record.key
      val valueInRaw = ccr.record.value
      val valueIn = io.circe.parser.decode[KInputPayload](valueInRaw).getOrElse(???)
      val outValueInt = doTheBusiness(valueIn.value)
      val valueOut: String = KOutputPayload(outValueInt).asJson.noSpaces
      import KSyntax._
      IO.sleep((random.nextInt(10) + 3).seconds) >>
        publisher
          .produceOne(Output.name, key, valueOut)
          .as(ccr.offset)
    }
    .evalMap(_.commit)
    .compile
    .drain

  override def run: IO[Unit] = publisher.use { kp =>
    app(kp)
  }
}
