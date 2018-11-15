# hbase4s

一个为scala编写的hbase orm工具。支持case class 类型到hbase的映射存取。

```scala
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


val a = Encode.gen[TableA]
val b = TableB("65.556", 33, "hello")
val src = TableA("HELLO world", "fff", b, 65536)
val Left(out) = a.encode(src)
val ed: Either[Seq[(HBTable,Put)],Array[Byte]] = a.encode(TableA("HELLO world", "fff", b, 65536))
val out = ed.left.get

val tables: HBTable => Table = _
//save to hbase
out.foreach(x => tables(x._1).put(x._2))
//read from hbase
val rowkey = out.head._2.getRow
val res:TableA = a.decode(rowkey).apply(tables)
println(res == src)
```
