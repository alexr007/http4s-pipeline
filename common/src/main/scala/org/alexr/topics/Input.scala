package org.alexr.topics

import io.circe.generic.AutoDerivation

object Input {
  val name = "input"

  case class KInputPayload(value: Int)
  object KInputPayload extends AutoDerivation
}
