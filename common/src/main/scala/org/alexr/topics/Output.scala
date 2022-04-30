package org.alexr.topics

import io.circe.generic.AutoDerivation

object Output {
  val name = "output"

  case class KOutputPayload(value: Int)
  object KOutputPayload extends AutoDerivation
}
