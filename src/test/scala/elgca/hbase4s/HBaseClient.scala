package elgca.hbase4s

import elgca.hbase4s.annotations.{HBRowKey, HBTable}
import elgca.hbase4s.serializer.Encode.HPut
import elgca.hbase4s.serializer._
import elgca.hbase4s.tool.HBaseTool._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes


case class TableB(
                   @HBRowKey
                   double: String,
                   a: Int,
                   b: String
                 )

case class TableA(
                   @HBRowKey
                   a: String,
                   @HBRowKey
                   str: String,
                   c: TableB,
                   f: BigInt)

object HBaseClient extends App {
  //  implicit val naming = DefaultNamingStrategy
  //  implicit val defaultFamily = HBFamily("f", 1)
  val a = Encode.gen[TableA]
  val b = TableB("65.556", 33, "hello")
  val ed: Either[Seq[(HBTable,Put)],Array[Byte]] = a.encode(TableA("HELLO world", "fff", b, 65536))
  val out = ed.left.get

  import org.apache.hadoop.hbase.HBaseConfiguration

  val conf = HBaseConfiguration.create()
  conf.set("hbase.zookeeper.quorum", "127.0.0.1:2181")
  val connection = ConnectionFactory.createConnection(conf)
  val names = connection.useAdmin {
    admin =>
      val htables = out.map(_._1).distinct
      htables.map {
        case htable@elgca.hbase4s.annotations.HBTable(name, families) =>
          val tableName = TableName.valueOf(name)
          if(!admin.tableExists(tableName)){
            val table = new HTableDescriptor(tableName)
            families.foreach {
              case elgca.hbase4s.annotations.HBFamily(x, version) =>
                table.addFamily(new HColumnDescriptor(x.getBytes).setMaxVersions(version))
            }
            admin.createTable(table)
          }
          name
      }
  }

  connection.useTables(names.distinct: _*) {
    tables =>
      out.foreach{
        case (table,put) =>
          tables.apply(table.name).put(put)
      }
  }
  val rowKey:Array[Byte] = out.head._2.getRow
  connection.useTables(names.distinct: _*) {
    tables =>
      val de = a.decode(rowKey).apply(x => tables.apply(x.name))
      println(de)
  }


  //  ConnectionFactory.createConnection(conf)
  //  val hBaseAdmin = new HBaseAdmin(cfg)
  //  ConnectionUtils.createShortCircuitConnection()
}
