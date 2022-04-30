package org.alexr

import cats.effect.Async
import cats.effect.Resource
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerSettings
import fs2.kafka.RecordSerializer
import scala.util.chaining.scalaUtilChainingOps

object KConfig {
  val servers = "kafka:9092"

  def producerSettings[
      F[_]: Async,
      K: RecordSerializer[F, *],
      V: RecordSerializer[F, *]
  ]: Resource[F, KafkaProducer[F, K, V]] =
    ProducerSettings[F, K, V]
      .withBootstrapServers(servers)
      .withEnableIdempotence(true)
      .withRetries(5)
      .pipe(KafkaProducer.resource[F, K, V](_))

}
