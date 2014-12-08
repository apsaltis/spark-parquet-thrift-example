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

resolvers ++= Seq(
  "sbt-plugin-releases-repo" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases",
  "sbt-idea-repository" at "http://mpeltonen.github.io/maven/",
  "bigtoast-github" at "http://bigtoast.github.com/repo/"
)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.github.bigtoast" % "sbt-thrift" % "0.7")

addSbtPlugin("com.cavorite" % "sbt-avro" % "0.3.2")

// https://github.com/mpeltonen/sbt-idea
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

// See https://github.com/scoverage/scalac-scoverage-plugin
// and https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.7.1")