name := "raf-udf-framwork"

version := "0.1.1"

scalaVersion := "2.11.12"

description := "Scala wrapper for Hive UDF functionality"

// EECI Java version
javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

// hive jar dependencies are not loaded
libraryDependencies ++= {
  val junitV = "4.12"
  val hiveV = "2.3.2"
  val scalatestV = "3.0.4"
  Seq(
//    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
//    "org.apache.spark" %% "spark-hive" % "2.2.0",
    "com.chuusai" %% "shapeless" % "2.3.2",
    "org.apache.hive" % "hive-exec" % hiveV % "provided" intransitive(),
    "org.apache.hive" % "hive-serde" % hiveV % "provided" intransitive(),
    "org.apache.hadoop" % "hadoop-core" % "1.0.4" % "provided" intransitive(),
    "org.scalatest" %% "scalatest" % scalatestV % Test
  )
}
