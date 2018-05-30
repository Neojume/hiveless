package hiveless

import org.apache.hadoop.hive.ql.exec.UDFArgumentException
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector

import scala.reflect._


object InspectorLookup {
  private var memory: Map[ClassTag[_], PrimitiveObjectInspector] = Map.empty

  def lookup[T : ClassTag]: PrimitiveObjectInspector = {
    val a = classTag[T]
    memory.getOrElse(a, {throw new UDFArgumentException(s"Could not lookup the right inspector for type ${a.toString()}")})
  }

  def add[T: ClassTag](objectInspector: PrimitiveObjectInspector): Unit = {
    memory = memory.updated(classTag[T], objectInspector)
  }
}
