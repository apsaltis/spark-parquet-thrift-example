///////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
///////////////////////////////////////////////////////////////////////////

import AssemblyKeys._
import com.github.bigtoast.sbtthrift.ThriftPlugin

assemblySettings

jarName in assembly := "SparkParquetAvroThrift.jar"

name := "SparkParquetAvroThrift"

version := "1.0"

fork := true

scalaVersion := "2.10.4"

seq(ThriftPlugin.thriftSettings: _*)

seq(sbtavro.SbtAvro.avroSettings : _*)

// Configure the desired Avro version.  sbt-avro automatically injects a libraryDependency.
//(version in avroConfig) := "1.7.6"

// Look for *.avsc etc. files in src/test/avro
(sourceDirectory in avroConfig) <<= (sourceDirectory in Compile)(_ / "avro")

(stringType in avroConfig) := "String"

// https://github.com/jrudolph/sbt-dependency-graph
net.virtualvoid.sbt.graph.Plugin.graphSettings

val bijectionVersion = "0.7.0"
val chillVersion = "0.5.1"
val sparkVersion = "1.1.1"
val stormVersion = "0.9.3"

libraryDependencies ++= Seq(
  // Spark dependencies.
  // Mark as provided if distributing to clusters.
  // Don't use 'provided' if running the program locally with `sbt run`.
  "org.apache.spark" %% "spark-core" % "1.1.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "1.1.0" % "provided",
//  "org.apache.hadoop" % "hadoop-client" % "2.4.0" % "provided" excludeAll(
//    ExclusionRule(organization = "org.jboss.netty"),
//    ExclusionRule(organization = "io.netty"),
//    ExclusionRule(organization = "org.eclipse.jetty"),
//    ExclusionRule(organization = "org.mortbay.jetty"),
//    ExclusionRule(organization = "org.ow2.asm"),
//    ExclusionRule(organization = "asm")
//  ),
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.apache.thrift" % "libthrift" % "0.9.1",
  "com.twitter" % "parquet-thrift" % "1.5.0",
  "com.twitter" % "parquet-avro" % "1.5.0",
  //"org.apache.avro" % "avro" % "1.7.6" classifier "hadoop2",
  "org.apache.avro" % "avro-mapred" % "1.7.6" classifier "hadoop2",
  "it.unimi.dsi" % "fastutil" % "6.1.0",
  "com.twitter" %% "bijection-core" % bijectionVersion,
  "com.twitter" %% "bijection-avro" % bijectionVersion,
  "com.twitter" %% "chill" % chillVersion,
  "com.twitter" %% "chill-avro" % chillVersion,
  "com.twitter" %% "chill-bijection" % chillVersion
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case s if s.endsWith(".class") => MergeStrategy.last
    case s if s.endsWith(".default") => MergeStrategy.last
    case s if s.endsWith(".map") => MergeStrategy.last
    case s if s.endsWith(".providers") => MergeStrategy.last
    case s if s.endsWith(".properties") => MergeStrategy.last
    case s if s.endsWith(".RSA") => MergeStrategy.last
    case s if s.endsWith("mailcap") => MergeStrategy.last
   // case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case x => old(x)
  }
}


libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Twitter" at "http://maven.twttr.com/",
  "bigtoast-github" at "http://bigtoast.github.com/repo/",
  "sbt-plugin-releases" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases"
)



javaOptions ++= Seq(
  "-Xms256m",
  "-Xmx512m",
  "-XX:+UseG1GC",
  "-XX:MaxGCPauseMillis=20",
  "-XX:InitiatingHeapOccupancyPercent=35",
  "-Djava.awt.headless=true",
  "-Djava.net.preferIPv4Stack=true")

javacOptions in Compile ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:unchecked",
  "-Xlint:deprecation")

scalacOptions ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

scalacOptions in Compile ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature",  // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code",
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

scalacOptions in Test ~= { (options: Seq[String]) =>
  options.filterNot(_ == "-Ywarn-value-discard").filterNot(_ == "-Ywarn-dead-code" /* to fix warnings due to Mockito */)
}

scalacOptions in ScoverageTest ~= { (options: Seq[String]) =>
  options.filterNot(_ == "-Ywarn-value-discard").filterNot(_ == "-Ywarn-dead-code" /* to fix warnings due to Mockito */)
}

publishArtifact in Test := false

parallelExecution in ThisBuild := false

// Write test results to file in JUnit XML format
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports/junitxml")

// Write test results to console.
//
// Tip: If you need to troubleshoot test runs, it helps to use the following reporting setup for ScalaTest.
//      Notably these suggested settings will ensure that all test output is written sequentially so that it is easier
//      to understand sequences of events, particularly cause and effect.
//      (cf. http://www.scalatest.org/user_guide/using_the_runner, section "Configuring reporters")
//
//        testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oUDT", "-eUDT")
//
//        // This variant also disables ANSI color output in the terminal, which is helpful if you want to capture the
//        // test output to file and then run grep/awk/sed/etc. on it.
//        testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oWUDT", "-eWUDT")
//
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-o")

// See https://github.com/scoverage/scalac-scoverage-plugin
instrumentSettings

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// We do not want to run the test when assembling because we prefer to chain the various build steps manually, e.g.
// via `./sbt clean test scoverage:test package packageDoc packageSrc doc assembly`.  Because, in this scenario, we
// have already run the tests before reaching the assembly step, we do not re-run the tests again.
//
// Comment the following line if you do want to (re-)run all the tests before building assembly.
test in assembly := {}

logLevel in assembly := Level.Warn