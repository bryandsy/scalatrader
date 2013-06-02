package net.sandipan.scalazmq.zmq

import org.jeromq.ZMQ.Context
import org.jeromq.ZMQ
import com.typesafe.config.Config
import net.sandipan.scalazmq.zmq.ContextComponent.ContextProvider

trait ContextComponent {

  def contextProvider: ContextProvider

}

object ContextComponent {

  val DEFAULT_IO_THREADS = 1

  case class ContextProvider(context: Context)

  def defaultContextProvider(config: Config): ContextProvider = {
    val ioThreads =
      try {
        config.getInt("ioThreads")
      } catch {
        case e: Exception => {
          DEFAULT_IO_THREADS
        }
      }
    ContextProvider(ZMQ.context(ioThreads))
  }

}