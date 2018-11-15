package elgca.hbase4s.serializer

import elgca.hbase4s.annotations.{HBColumn, HBFamily, HBTable, HMeta}
import magnolia._
import org.apache.hadoop.hbase.client.{Get, Put, Table}
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.mutable.ListBuffer
import scala.language.experimental.macros

trait Encode[T] {

  import Encode._

  def isRecord: Boolean = false

  def encode(t: T): HPut

  def decode(t: HResult): (HBTable => Table) => T
}

object Encode extends EncodeImplicit {
  type Field = Array[Byte]
  type IPut = (HBTable, Put)
  type HPut = Either[Seq[IPut], Field]
  type RowKey = Array[Byte]
  type HResult = Array[Byte]
  type Typeclass[T] = Encode[T]

  def buildIPut(hBTable: HBTable, put: Put): IPut = {
    (hBTable, put)
  }

  def combine[T](caseClass: CaseClass[Typeclass, T])
                (implicit namingStrategy: NamingStrategy, defaultFamily: HBFamily): Typeclass[T] = {
    new Typeclass[T] {
      override def isRecord: Boolean = true

      private val fields = caseClass.parameters.map { p =>
        val mt = HMeta(p.annotations)
        mt.getColumn.getOrElse(HBColumn(defaultFamily.name, namingStrategy.to(p.label))) -> mt.isRowKey
      }.toVector
      private val keys = fields.zipWithIndex.filter(_._1._2)
      private val keysIndex = keys.map(_._2)
      private val table = HMeta(caseClass.annotations).getTable
        .map(t => if (t.families.isEmpty) t.copy(families = Array(defaultFamily)) else t)
        .getOrElse(HBTable(caseClass.typeName.short, Array(defaultFamily)))
      assert(keys.nonEmpty, "必须含有rowkey")

      override def encode(t: T): HPut = {
        if (caseClass.isObject) {
          Right(Bytes.toBytes(t.toString))
        } else {
          val data = caseClass.parameters.map { p =>
            Option(p.dereference(t)).orElse(p.default) match {
              case Some(value) => fields(p.index)._1 -> p.typeclass.encode(value)
              case None => null
            }
          }
          val rowKey = keysIndex.flatMap(i => data(i) match {
            case null => Seq.empty
            case (_, Left(l)) => l.head._2.getRow
            case (_, Right(r)) => r
          }).toArray
          val put = new Put(rowKey)
          val buffer = new ListBuffer[IPut]
          buffer.append(buildIPut(table, put))
          data.foreach {
            case null =>
            case (f, Right(v)) => put.addColumn(f.family.getBytes, f.column.getBytes, v)
            case (f, Left(v)) =>
              buffer.appendAll(v)
              val key = v.head._2.getRow
              put.addColumn(f.family.getBytes, f.column.getBytes, key)
          }
          put.addColumn(table.families.head.name.getBytes(), "caseClass".getBytes(), caseClass.typeName.full.getBytes)
          Left(buffer)
        }
      }

      override def decode(t: HResult): (HBTable => Table) => T = {
        import elgca.hbase4s.tool.HBaseTool._
        val rowKey: RowKey = t
        val get = new Get(rowKey)
        fields.foreach { case (col, _) =>
          get.addColumn(col.family.getBytes, col.column.getBytes)
        }
        get.addColumn(table.families.head.name.getBytes(), "caseClass".getBytes())
        (tables: HBTable => Table) => {
          val tb = tables(table)
          val rs = tb.get(get)
          val cels = rs.rawCells().toIterator.map(c => c.qualifier -> c.value).toMap
          caseClass.construct(p => {
            val col = fields(p.index)._1
            val param = cels.get(col.column)
            param.map(p.typeclass.decode).map(_.apply(tables)).getOrElse(null)
          })
        }
      }
    }
  }

  //  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = {
  //    new Typeclass[T] {
  //      override def isRecord: Boolean = true
  //
  //      override def encode(t: T): HPut = {
  //        sealedTrait.dispatch(t) {
  //          handle => handle.typeclass.encode(handle.cast(t))
  //        }
  //      }
  //
  //      override def decode(t: HResult): (HBTable => Table) => T = ???
  //    }
  //  }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}