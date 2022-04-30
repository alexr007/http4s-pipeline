package org.alexr

import cats.effect.IO
import cats.effect.IOApp

object ProcessorEngine extends IOApp.Simple {

  def doTheBusiness(value: Int): Int = value + 1

  // start publisher
  // start consumer
  // run them

  override def run: IO[Unit] = ???
}
