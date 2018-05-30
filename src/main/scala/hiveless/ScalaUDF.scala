package hiveless

import org.apache.hadoop.hive.ql.exec.UDFArgumentException
import org.apache.hadoop.hive.ql.metadata.HiveException
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF.DeferredObject
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector


abstract class ScalaUDF[Input, Output](
  implicit
  inputWrapper: HiveWrapper[Input],
  outputWrapper: HiveWrapper[Output]
) extends GenericUDF {

  def transform(input: Input): Output

  @throws(classOf[UDFArgumentException])
  override def initialize(arguments: Array[ObjectInspector]): ObjectInspector = {
    inputWrapper match {
      case hsw : HiveStructWrapper[Input] => hsw.verifyInspectorList(arguments.toList)
      case _ if arguments.length == 1 => inputWrapper.verifyInspector(arguments.head)
      case _ => throw new UDFArgumentException("Unrecognized input shape")
    }

    outputWrapper.getInspector
  }

  @throws(classOf[HiveException])
  override def evaluate(arguments: Array[DeferredObject]): AnyRef = {
    val input: Input = inputWrapper match {
      case hsw: HiveStructWrapper[Input] => hsw.fromObjectList(arguments.map(_.get()).toList)
      case _ if arguments.length == 1 => inputWrapper.fromObject(arguments.head.get())
      case _ => throw new HiveException("Unrecognized input shape")
    }

    val out: Output = transform(input)
    outputWrapper.toObject(out)
  }

  override def getDisplayString(children: Array[String]): String = children.mkString("(", ",", ")")
}