package scalafix.internal.sbt

import java.{util => ju}

import scala.util.Try

/** A basic thread-safe cache without any eviction. */
class BlockingCache[K, V] {

  // Number of keys is expected to be very small so the global lock should not be a bottleneck
  private val underlying = ju.Collections.synchronizedMap(new ju.HashMap[K, V])

  private case class SkipUpdate(prev: V) extends Exception

  // Evaluations of `transform` are guaranteed to be sequential for the same key
  def compute(key: K, transform: Option[V] => Option[V]): V = {
    Try(
      underlying.compute(
        key,
        new ju.function.BiFunction[K, V, V] {
          override def apply(key: K, prev: V): V = {
            transform(Option(prev)).getOrElse(throw SkipUpdate(prev))
          }
        }
      )
    ).fold(
      {
        case SkipUpdate(prev) => prev
        case ex => throw ex
      },
      identity
    )
  }
}
