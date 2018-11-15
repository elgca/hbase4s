package elgca.hbase4s.serializer

import elgca.hbase4s.annotations.HBTable
import elgca.hbase4s.serializer.Encode.{Field, HPut, HResult}
import org.apache.hadoop.hbase.client.Table
import org.apache.hadoop.hbase.util.Bytes

trait EncodeImplicit {

  private def const[@specialized T](a: T => Field, b: Field => T) = {
    new Encode[T] {
      override def encode(t: T): HPut = Right(a(t))

      override def decode(t: HResult): (HBTable => Table) => T = _ => {
        b(t)
      }
    }
  }

  implicit val string = const[String](Bytes.toBytes, Bytes.toString)
  implicit val byte = const[Byte](x => Array(x), _.head)
  implicit val short = const[Short](Bytes.toBytes, Bytes.toShort)
  implicit val int = const[Int](Bytes.toBytes, Bytes.toInt)
  implicit val long = const[Long](Bytes.toBytes, Bytes.toLong)
  implicit val bigInt = const[BigInt](x => x.toByteArray, x => BigInt(x))
  implicit val float = const[Float](Bytes.toBytes, Bytes.toFloat)
  implicit val double = const[Double](Bytes.toBytes, Bytes.toDouble)
  implicit val decimal = const[BigDecimal](x => Bytes.toBytes(x.underlying()), x => Bytes.toBigDecimal(x))
}
