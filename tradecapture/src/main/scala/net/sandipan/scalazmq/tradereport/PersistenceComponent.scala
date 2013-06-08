package net.sandipan.scalazmq.tradereport

import net.sandipan.scalazmq.common.model.Id

trait PersistenceComponent[T <: Id[String]] {

  def repository: Repository

  trait Repository {
    def persist(item: T)
  }

  class InMemoryRepository extends Repository {

    private var data: Map[T#Type, T] = Map()

    def persist(item: T) {
      data += (item.id -> item)
    }

  }

}
