package net.sandipan.scalatrader.marketdata

import com.typesafe.config.ConfigFactory

object Main extends App {

  val config = ConfigFactory.load()
  val registry = new ComponentRegistry(config)

  try {
    registry.marketDataSender.startSendingData()
  } finally {
    registry.publisher.closeAllSockets()
  }

}
