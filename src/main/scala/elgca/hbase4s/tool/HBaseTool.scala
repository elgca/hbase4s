package elgca.hbase4s.tool


import org.apache.commons.lang3.ArrayUtils
import org.apache.hadoop.hbase.client.{Result, _}
import org.apache.hadoop.hbase.filter.Filter
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{Cell, TableName}

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.Try

object HBaseTool extends Serializable {
  def use[C <: AutoCloseable, T](s: C)(op: C => T): T = {
    try {
      op(s)
    } finally {
      if (s != null) s.close()
    }
  }

  @inline implicit def name2Table(name: String): TableName = TableName.valueOf(name)

  @inline implicit def str2Bytes(str: String): Array[Byte] = Bytes.toBytes(str)

  implicit class RichConnection(val connection: Connection) {
    self =>
    def toRich: RichConnection = self

    def useTable[T](name: String)(op: Table => T): T = {
      use(connection.getTable(name))(op)
    }

    def useTables[T](names: String*)(op: Map[String, Table] => T): T = {
      val tables = names.map(x => x -> Try(connection.getTable(x))).toMap
      try op(tables.mapValues(_.get))
      finally tables.foreach(_._2.foreach(_.close()))
    }

    def useAdmin[T](op: Admin => T): T = {
      use(connection.getAdmin)(op)
    }

  }

  implicit class RichResult(res: Result) {
    def toMap: Map[String, Map[String, String]] = {
      res.getNoVersionMap.asScala.map {
        case (k, v) =>
          Bytes.toString(k) -> v.asScala.map {
            case (k2, v2) =>
              Bytes.toString(k2) -> Bytes.toString(v2)
          }.toMap
      }
    }.toMap
  }

  implicit class RichCell(cell: Cell) {
    @inline def value: Array[Byte] = ArrayUtils.subarray(cell.getRowArray, cell.getValueOffset, cell.getValueLength)

    @inline def qualifier: String = Bytes.toString(ArrayUtils.subarray(cell.getRowArray, cell.getQualifierOffset, cell.getQualifierLength))

    @inline def family: String = Bytes.toString(ArrayUtils.subarray(cell.getRowArray, cell.getFamilyOffset, cell.getFamilyLength))
  }

  implicit class RichTable(table: Table) {
    def get(rowKey: String): Result = {
      get(rowKey, Map.empty[String, List[String]], 1)
    }

    def get(rowKey: String, columns: Map[String, List[String]], version: Int): Result = {
      get(str2Bytes(rowKey), columns, version)
    }

    def get(rowKey: Array[Byte], columns: Map[String, List[String]], version: Int): Result = {
      val gt = new Get(rowKey)
        .setMaxVersions(version)
      columns.foreach {
        case (f, cs) =>
          gt.addFamily(f)
          cs.foreach { c => gt.addColumn(f, c) }
      }
      table.get(gt)
    }

    def scan(start: String, stop: String): Iterable[Result] = {
      scan(start, stop, Map.empty[String, List[String]], 1)
    }

    def scan(start: String, stop: String, columns: Map[String, List[String]], version: Int): Iterable[Result] = {
      scan(str2Bytes(start), str2Bytes(stop), columns, version)
    }

    def scan(start: Array[Byte], stop: Array[Byte], columns: Map[String, List[String]], version: Int): Iterable[Result] = {
      scan(start, stop, columns, null, version)
    }

    def scan(start: Array[Byte], stop: Array[Byte], columns: Map[String, List[String]], filter: Filter, version: Int): Iterable[Result] = {
      //val cpm = Bytes.compareTo(start, stop)
      val scn =
        new Scan()
          .setFilter(filter)
          .setStartRow(start)
          .setStopRow(stop)
          .setMaxVersions(version)
      columns.foreach {
        case (f, cs) =>
          scn.addFamily(f)
          cs.foreach { c => scn.addColumn(f, c) }
      }
      table.getScanner(scn).asScala
    }

    def put(puts: Seq[Put]): Unit = {
      table.put(puts.asJava)
    }
  }

}