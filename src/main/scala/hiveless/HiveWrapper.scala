package hiveless


import org.apache.hadoop.hive.ql.exec.UDFArgumentException
import org.apache.hadoop.hive.serde2.`lazy`.LazyInteger
import org.apache.hadoop.hive.serde2.objectinspector.primitive._
import org.apache.hadoop.hive.serde2.objectinspector._
import org.apache.hadoop.io.IntWritable
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}
import shapeless.labelled.{FieldType, field}

import scala.collection.JavaConverters._

trait HiveWrapper[A] {
  def getInspector: ObjectInspector
  def verifyInspector(oi: ObjectInspector): Unit
  def toObject(a: A): Object
  def fromObject(obj: Object): A
}

trait HiveStructWrapper[A] extends HiveWrapper[A] {
  def getInspector: StructObjectInspector
  def getFieldInspectors: Map[String, ObjectInspector]
  def toObject(a: A): Array[Object]
  def fromObjectList(objList: List[Object]): A
  def verifyInspectorList(oiList: List[ObjectInspector]): Unit
}

object HiveWrapper {

  implicit val intWrapper: HiveWrapper[Int] = new HiveWrapper[Int] {
    override def getInspector: PrimitiveObjectInspector = PrimitiveObjectInspectorFactory.javaIntObjectInspector//InspectorLookup.lookup[Int]

    override def fromObject(obj: Object): Int = {
//      getInspector.getPrimitiveWritableObject(obj).asInstanceOf[IntWritable].get
      obj match {
        case lzy : LazyInteger => lzy.getWritableObject.asInstanceOf[IntWritable].get
        case i : Integer => i
        case x => throw new UDFArgumentException(s"Dont know what to do with: $x")
      }
    }

    override def verifyInspector(oi: ObjectInspector): Unit = oi match {
      //case poi: PrimitiveObjectInspector => InspectorLookup.add[Int](poi)
      case ioi: IntObjectInspector => InspectorLookup.add[Int](ioi)
      case x => throw new UDFArgumentException(s"Expected primitive inspector for int, but found $x")
    }

    override def toObject(a: Int): Integer = a.asInstanceOf[Integer]
  }

  implicit val doubleWrapper: HiveWrapper[Double] = new HiveWrapper[Double] {
    override def getInspector: JavaDoubleObjectInspector = PrimitiveObjectInspectorFactory.javaDoubleObjectInspector

    override def fromObject(obj: Object): Double = getInspector.get(obj)

    override def toObject(a: Double): java.lang.Double = a.asInstanceOf[java.lang.Double]

    override def verifyInspector(oi: ObjectInspector): Unit = oi match {
      case joi: DoubleObjectInspector => InspectorLookup.add[Double](joi)
      case x => throw new UDFArgumentException(s"Expected primitive inspector for double, but found $x")
    }
  }

  implicit val stringWrapper: HiveWrapper[String] = new HiveWrapper[String] {
    override def getInspector: JavaStringObjectInspector = PrimitiveObjectInspectorFactory.javaStringObjectInspector

    override def fromObject(obj: Object): String = getInspector.getPrimitiveJavaObject(obj)

    override def toObject(a: String): String = a

    override def verifyInspector(oi: ObjectInspector): Unit = oi match {
      case soi: StringObjectInspector => InspectorLookup.add[String](soi)
      case x => throw new UDFArgumentException(s"Expected primitive inspector for string, but found $x")
    }
  }

  implicit def listWrapper[A](implicit elementWrapper: HiveWrapper[A]): HiveWrapper[List[A]] = {
    new HiveWrapper[List[A]] {
      override def getInspector: StandardListObjectInspector = {
        ObjectInspectorFactory.getStandardListObjectInspector(elementWrapper.getInspector)
      }

      override def fromObject(obj: Object): List[A] = {
        getInspector.getList(obj).toArray.map(elementWrapper.fromObject).toList
      }

      override def toObject(a: List[A]): Array[Object] = a.map(elementWrapper.toObject).toArray

      override def verifyInspector(oi: ObjectInspector): Unit = Unit // TODO: Implement
    }
  }

  implicit def genericWrapper[T, Repr](
    implicit
    lgen: LabelledGeneric.Aux[T, Repr],
    reprWrapper: HiveWrapper[Repr]
  ): HiveWrapper[T] = new HiveWrapper[T] {
    override def getInspector: ObjectInspector = reprWrapper.getInspector

    override def fromObject(obj: Object): T = lgen.from(reprWrapper.fromObject(obj))

    override def toObject(a: T): Object = reprWrapper.toObject(lgen.to(a))

    override def verifyInspector(oi: ObjectInspector): Unit = reprWrapper.verifyInspector(oi)
  }

  implicit val hnilWrapper: HiveStructWrapper[HNil] = new HiveStructWrapper[HNil] {
    override def getInspector: StructObjectInspector = {
      // Intentionally unimplemented, should never be called
      ???
    }
    override def fromObject(obj: Object): HNil.type = HNil
    override def toObject(a: HNil): Array[Object] = Array.emptyObjectArray
    override def getFieldInspectors: Map[String, ObjectInspector] = Map.empty
    override def fromObjectList(objList: List[Object]): HNil = HNil

    override def verifyInspector(oi: ObjectInspector): Unit = Unit
    override def verifyInspectorList(oiList: List[ObjectInspector]): Unit = Unit
  }

  implicit def structWrapper[Key <: Symbol, Head, Tail <: HList](
    implicit
    key: Witness.Aux[Key],
    headWrapper: Lazy[HiveWrapper[Head]],
    tailWrapper: HiveStructWrapper[Tail]
  ): HiveStructWrapper[FieldType[Key, Head] :: Tail] = {
    new HiveStructWrapper[FieldType[Key, Head] :: Tail] {
      override def getInspector: StandardStructObjectInspector = {
        val fieldInspectorMap = getFieldInspectors
        val fieldNames = fieldInspectorMap.keys.toList.asJava
        val fieldInspectors = fieldInspectorMap.values.toList.asJava
        ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldInspectors)
      }

      override def getFieldInspectors: Map[String, ObjectInspector] = {
        tailWrapper.getFieldInspectors.updated(key.value.name, headWrapper.value.getInspector)
      }

      override def fromObject(obj: Object): FieldType[Key, Head] :: Tail = {
        fromObjectList(getInspector.getStructFieldsDataAsList(obj).asScala.toList)
      }

      override def toObject(a: FieldType[Key, Head] :: Tail): Array[Object] = {
        val headObject = headWrapper.value.toObject(a.head)
        val tailArray = tailWrapper.toObject(a.tail)
        tailArray ++ Array(headObject)
      }

      override def fromObjectList(objList: List[Object]): FieldType[Key, Head] :: Tail = {
        field[Key](headWrapper.value.fromObject(objList.head)) :: tailWrapper.fromObjectList(objList.tail)
      }

      override def verifyInspector(oi: ObjectInspector): Unit = {
        oi match {
          case ssi: StandardStructObjectInspector =>
            verifyInspectorList(ssi.getAllStructFieldRefs.asScala.map(_.getFieldObjectInspector).toList)
          case x => throw new UDFArgumentException(s"Expected structInspector, but got $x")
        }
      }

      override def verifyInspectorList(oiList: List[ObjectInspector]): Unit = {
        field[Key](headWrapper.value.verifyInspector(oiList.head))
        tailWrapper.verifyInspectorList(oiList.tail)
      }
    }
  }

  implicit def genericStructWrapper[T, Repr](
    implicit
    lgen: LabelledGeneric.Aux[T, Repr],
    reprWrapper: HiveStructWrapper[Repr]
  ): HiveStructWrapper[T] = new HiveStructWrapper[T] {
    override def getInspector: StructObjectInspector = reprWrapper.getInspector

    override def fromObject(obj: Object): T = lgen.from(reprWrapper.fromObject(obj))

    override def toObject(a: T): Array[Object] = reprWrapper.toObject(lgen.to(a))

    override def getFieldInspectors: Map[String, ObjectInspector] = reprWrapper.getFieldInspectors

    override def fromObjectList(objList: List[Object]): T = lgen.from(reprWrapper.fromObjectList(objList))

    override def verifyInspectorList(oiList: List[ObjectInspector]): Unit = reprWrapper.verifyInspectorList(oiList)

    override def verifyInspector(oi: ObjectInspector): Unit = reprWrapper.verifyInspector(oi)
  }
}
