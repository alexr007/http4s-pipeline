package org.alexr

import cats.effect.Async
import cats.effect.Resource
import fs2.kafka.AutoOffsetReset
import fs2.kafka.ConsumerSettings
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerSettings
import fs2.kafka.RecordDeserializer
import fs2.kafka.RecordSerializer
import scala.util.chaining.scalaUtilChainingOps

object KConfig {
  val servers = "kafka:9092"

  def producer[
      F[_]: Async,
      K: RecordSerializer[F, *],
      V: RecordSerializer[F, *]
  ]: Resource[F, KafkaProducer[F, K, V]] =
    ProducerSettings[F, K, V]
      .withBootstrapServers(servers)
      .withEnableIdempotence(true)
      .withRetries(5)
      .pipe(KafkaProducer.resource[F, K, V](_))

  def consumerSettings[
      F[_],
      K: RecordDeserializer[F, *],
      V: RecordDeserializer[F, *]
  ](groupId: String) =
    ConsumerSettings[F, K, V]
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withBootstrapServers(servers)
      .withGroupId(groupId)

}
