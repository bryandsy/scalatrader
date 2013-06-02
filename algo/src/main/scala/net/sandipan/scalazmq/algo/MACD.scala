package net.sandipan.scalazmq.algo

import net.sandipan.scalazmq.common.model.{SellSignal, BuySignal, Signal, MarketData}
import com.typesafe.config.Config
import net.sandipan.scalazmq.algo.MACD.Params

/**
 * A simplified MACD. Note: for pedagogical purposes, just using simple moving averages rather than
 * exponential moving averages.
 */
class MACD(params: Params) extends Algorithm {

  import MACD._

  private var dataMap: Map[Symbol, SymbolData] = Map()

  def submit(marketData: MarketData): Option[Signal] = {
    pushData(marketData)
    checkForCrossover(marketData.symbol)
  }

  /**
   * If any "None's" in a sequence, the return value will be none.
   */
  def flatten[X](seq: List[Option[X]]): Option[List[X]] = {
    seq.foldLeft(Some(Nil): Option[List[X]])((x, y) => y match {
      case None => None
      case Some(n) => Some(n :: x.get)
    })
  }

  private def pushData(data: MarketData) {

    val symbolData = dataMap.getOrElse(data.symbol, Nil).take(params.slow - 1)

    def averagePrice(points: Int, f1: DataPoint => Option[Money], f2: MarketData => Money): Option[Money] = {
      if (symbolData.length < points - 1)
        None
      else {
        val valueVector = symbolData.take(points - 1) map f1
        for {x <- flatten(valueVector)}
          yield {(x.foldLeft(BigDecimal(0))(_ + _) + f2(data)) / points}
      }
    }

    val slowAverage = averagePrice(params.slow, d => Some(d.price), _.bid)
    val fastAverage = averagePrice(params.fast, d => Some(d.price), _.bid)

    val macd = for {x <- fastAverage
                    y <- slowAverage} yield (x - y)

    val signal = for {x <- macd
                      y <- averagePrice(params.sig, _.macd, _ => x)} yield (y)

    val difference = for {x <- macd
                          y <- signal} yield (x - y)

    dataMap += data.symbol -> (DataPoint(data.bid, slowAverage, fastAverage, macd, signal, difference) :: symbolData)
  }

  private def checkForCrossover(symbol: String): Option[Signal] = {
    val symbolData = dataMap(symbol)

    def isBuySignal(latest: BigDecimal, previous: BigDecimal): Boolean =
      (latest >= 0 && previous < 0 || (latest > 0 && previous <= 0))

    def isSellSignal(latest: BigDecimal, previous: BigDecimal): Boolean =
      ((latest <= 0 && previous > 0) || (latest < 0 && previous >= 0))

    for {head <- symbolData.headOption
         prev <- symbolData.tail.headOption
         latestSignal <- head.difference
         prevSignal <- prev.difference
           if (isBuySignal(latestSignal, prevSignal)
             || isSellSignal(latestSignal, prevSignal)) } yield {

      if (isBuySignal(latestSignal, prevSignal))
        BuySignal(symbol)
      else
        SellSignal(symbol)
    }
  }

}

object MACD {

  type Slow = Int
  type Fast = Int
  type Sig = Int
  type Symbol = String
  type Money = BigDecimal
  type Price = Money

  type SymbolData = List[DataPoint]
  case class Params(fast: Int, slow: Int, sig: Int)
  case class DataPoint(price: Money, slowSMA: Option[Money], fastSma: Option[Money], macd: Option[Money], signal: Option[BigDecimal], difference: Option[BigDecimal])

  def fromConfig(config: Config): MACD = {

    val params: Params = {
      val macdParams = config.getConfig("macdParameters")
      val slow = macdParams.getInt("slow")
      val fast = macdParams.getInt("fast")
      val signal = macdParams.getInt("signal")
      Params(slow, fast, signal)
    }

    new MACD(params)

  }

}