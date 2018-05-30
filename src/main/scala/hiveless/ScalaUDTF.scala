package hiveless

import org.apache.hadoop.hive.ql.exec.UDFArgumentException
import org.apache.hadoop.hive.ql.metadata.HiveException
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF
import org.apache.hadoop.hive.serde2.objectinspector._

abstract class ScalaUDTF [Input, Output](
  implicit
  inputWrapper: HiveStructWrapper[Input],
  outputWrapper: HiveStructWrapper[Output]
) extends GenericUDTF {

  @throws(classOf[UDFArgumentException])
  override def initialize(arguments: Array[ObjectInspector]): StructObjectInspector = {
    inputWrapper.verifyInspectorList(arguments.toList)
    outputWrapper.getInspector
  }

  def transform(input: Input): Iterable[Output]

  @throws(classOf[HiveException])
  override def process(args: Array[AnyRef]): Unit = {
    val input: Input = inputWrapper.fromObjectList(args.toList)
    val outputs: Iterable[Output] = transform(input)
    outputs.foreach(output => forward(outputWrapper.toObject(output)))
  }

  @throws(classOf[HiveException])
  override def close(): Unit = {}
}

