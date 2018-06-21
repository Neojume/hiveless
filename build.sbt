
lazy val commonSettings = Seq(
  organization := "com.github.neojume",
  scalaVersion := "2.11.12",
  //scalaVersion := "2.12.6",
  //crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
  javacOptions ++= Seq(
    "-source", "1.7",
    "-target", "1.7"
  ),
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.7",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused"
  ),
  libraryDependencies ++= {
    val hiveV = "2.3.2"
    Seq(
      // Provided dependencies: should be in the Hive execution environment
      "org.apache.hive" % "hive-exec" % hiveV % Provided intransitive(),
      "org.apache.hive" % "hive-serde" % hiveV % Provided intransitive(),
      "org.apache.hadoop" % "hadoop-core" % "1.0.4" % Provided intransitive(),

      // Actual dependencies
      "com.chuusai" %% "shapeless" % "2.3.2",
      "org.scala-lang" % "scala-reflect" % "2.11.1",

      // Test dependencies
      "org.scalatest" %% "scalatest" % "3.0.4" % Test
    )
  }
)

lazy val publishSettings = Seq(
  publishArtifact in Test := false,
  bintrayRepository := "neojume",
  pomIncludeRepository :=  { _ => false },
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  pomExtra in Global := {
    <url>https://github.com/neojume/hiveless</url>
      <scm>
        <connection>scm:git@github.com:neojume/hiveless.git</connection>
        <developerConnection>scm:git@github.com:neojume/hiveless.git</developerConnection>
        <url>https://github.com/neojume/hiveless</url>
      </scm>
      <developers>
        <developer>
          <id>neojume</id>
          <name>Steven Laan</name>
        </developer>
      </developers>
  }
)

lazy val hiveless = (project in file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "hiveless",
    description := "Scala wrapper for Hive UDF functionality"
  )