package net.sandipan.scalatrader.algo

import com.typesafe.config.ConfigFactory

object Main extends App {

  val config = ConfigFactory.load()
  val registry = new ComponentRegistry(config)

  try {
    registry.runner.run()
  } finally {
    registry.subscription.closeAllSockets()
  }

}
