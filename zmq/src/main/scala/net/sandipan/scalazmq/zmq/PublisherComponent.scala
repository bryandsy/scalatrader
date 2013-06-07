package net.sandipan.scalazmq.zmq

import org.jeromq.ZMQ
import net.sandipan.scalazmq.common.components.ConfigComponent
import net.sandipan.scalazmq.common.util.HasLogger
import net.sandipan.scalazmq.zmq.serialization.{Topic, Serializer}
import org.jeromq.ZMQ.Socket

trait PublisherComponent[T] {
  this: ContextComponent with ConfigComponent =>

  def publisher: Publisher

  class Publisher extends HasLogger with HasZmqSockets {

    private val socketAddr = config.getString(PublisherComponent.ADDRESS)

    private val socket = new ThreadLocal[Socket]() {
      override def initialValue(): Socket = {
        val s = contextProvider.context.socket(ZMQ.PUB)
        if (!s.connect(socketAddr))
          throw new RuntimeException("Could not BIND to ZMQ socket %s".format(socketAddr))
        registerSocket(s)
        s
      }
    }

    def publish(data: T)(implicit serializer: Serializer[T], topic: Topic[T]) {
      if (!socket.get.sendMore(topic.value))
        log.error("Oops.. unable to send topic information.")

      if (!socket.get.send(serializer.serialize(data)))
        log.error("Oops.. unable to send %s".format(data))
    }

  }

}

object PublisherComponent {

  val ADDRESS = "zmq.publishAddress"

}