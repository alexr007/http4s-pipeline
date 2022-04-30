package org.alexr

import cats.Monad
import cats.implicits.catsSyntaxFlatten
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerRecord
import fs2.kafka.ProducerRecords

object KSyntax {

  implicit class ProduceSyntax[F[_]: Monad, K, V](producer: KafkaProducer[F, K, V]) {

    def produceOne(topic: String, key: K, value: V) =
      producer
        .produce(
          ProducerRecords.one(
            ProducerRecord(
              topic,
              key,
              value
            )
          )
        )
        .flatten

  }

}
