name := """thesis-13520173"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"
val sparkVersion: String = "2.1.1"
val stanfordVersion: String = "3.8.0"
val jacksonVersion: String = "2.9.0.pr3"
val poiVersion: String = "3.16"

libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "com.orientechnologies" % "orientdb-graphdb" % "2.2.21" // Orient DB
// Hadoop
dependencyOverrides += "org.apache.hadoop" % "hadoop-common" % "2.8.1"
// Apachi Spark and Stanford tokenize
libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % sparkVersion,
  "org.apache.spark" % "spark-sql_2.11" % sparkVersion,
  "org.apache.spark" % "spark-mllib_2.11" % sparkVersion,
  "com.crealytics" % "spark-excel_2.11" % "0.8.4"
)

// Excel file Apache Poi
libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % poiVersion,
  "org.apache.poi" % "poi-ooxml" % poiVersion
)

dependencyOverrides += "com.google.protobuf" % "protobuf-java" % "3.3.1"
// Stanford tokenizer
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % stanfordVersion
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % stanfordVersion classifier "models" // Need to use when use models tokenize and stemmer
// Jackson for apachi spark - use when comflict with spark
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
dependencyOverrides += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % jacksonVersion
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion
// https://mvnrepository.com/artifact/de.unkrig.jdisasm/jdisasm
libraryDependencies += "de.unkrig.jdisasm" % "jdisasm" % "1.0.0"

// Neo4j
libraryDependencies += "org.neo4j.driver" % "neo4j-java-driver" % "1.4.3"
// NodeJs framework
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

//org.jetbrains.sbt.StructureKeys.sbtStructureOptions in Global := "download resolveClassifiers resolveSbtClassifiers"