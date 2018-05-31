name := "hiveless"

version := "0.1.1"

scalaVersion := "2.11.12"

description := "Scala wrapper for Hive UDF functionality"

// EECI Java version
javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

// Hive jar dependencies are not loaded
libraryDependencies ++= {
  val hiveV = "2.3.2"
  Seq(
    // Provided dependencies: should be in the Hive execution environment
    "org.apache.hive" % "hive-exec" % hiveV % Provided intransitive(),
    "org.apache.hive" % "hive-serde" % hiveV % Provided intransitive(),
    "org.apache.hadoop" % "hadoop-core" % "1.0.4" % Provided intransitive(),

    // Actual dependencies
    "com.chuusai" %% "shapeless" % "2.3.2",

    // Test dependencies
    "org.scalatest" %% "scalatest" % "3.0.4" % Test
  )
}

scalacOptions += "-Ylog-classpath"