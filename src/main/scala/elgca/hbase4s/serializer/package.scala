package elgca.hbase4s

import elgca.hbase4s.annotations.HBFamily

package object serializer {
  implicit val naming = DefaultNamingStrategy
  implicit val defaultFamily = HBFamily("f", 1)
}
