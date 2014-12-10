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