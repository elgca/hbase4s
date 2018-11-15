import Dependencies._

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")

lazy val inBuild = Seq(
  organization := "elgca",
  scalaVersion := "2.12.6",
  version := "0.1.0-SNAPSHOT"
)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(inBuild),
    name := "hbase4s",
    libraryDependencies ++= {
      Seq(
        scalaTest % Test,
        "com.propensive" %% "magnolia" % "0.9.1",
        "org.apache.hbase" % "hbase-common" % "1.2.1",
        "org.apache.hbase" % "hbase-client" % "1.2.1",
        "org.apache.hbase" % "hbase-client" % "1.2.1",
        "org.apache.commons" % "commons-lang3" % "3.8.1",
        "org.apache.avro" % "avro" % "1.8.2",
        "com.github.nikita-volkov" % "sext" % "[0.2.4,0.3)",
      "com.github.nikita-volkov" % "embrace" % "[0.1.5,0.2)" withSources(),
        "org.sorm-framework" % "sorm" % "0.3.21",
        "org.apache.hadoop" % "hadoop-common" % "2.7.0"

      )
    }
  )

//lazy val macroBase = (project in file("macro-bases")).
//  settings(
//    inThisBuild(inBuild),
//    name := "macro bases",
//    libraryDependencies ++= {
//      Seq(
//        scalaTest % Test,
//        "com.propensive" %% "magnolia" % "0.9.1",
//        "com.propensive" %% "mercator" % "0.1.1"
//      )
//    }
//  )