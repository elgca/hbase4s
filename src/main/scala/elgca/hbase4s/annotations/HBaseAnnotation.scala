package elgca.hbase4s.annotations

import scala.annotation.StaticAnnotation

sealed trait HBaseAnnotation extends StaticAnnotation

case class HBTable(name: String, families: Array[HBFamily] = Array.empty){
  override def toString: String = "HBTable( name = " + name + ", families = " + families.mkString("[",",","])")
}

case class HBFamily(name: String, version: Int) extends HBaseAnnotation

case class HBRowKey() extends HBaseAnnotation

//case class HBColumnMultiVersion() extends HBaseAnnotation

case class HBColumn(family: String = "", column: String = null) extends HBaseAnnotation

case class HMeta(ann: Seq[Any]) {
  def getTable: Option[HBTable] = ann.find(_.isInstanceOf[HBTable]).map(_.asInstanceOf[HBTable])

  def getFamily: Option[HBFamily] = ann.find(_.isInstanceOf[HBFamily]).map(_.asInstanceOf[HBFamily])

  def getColumn: Option[HBColumn] = ann.find(_.isInstanceOf[HBColumn]).map(_.asInstanceOf[HBColumn])

  def isRowKey: Boolean = ann.exists(_.isInstanceOf[HBRowKey])
}