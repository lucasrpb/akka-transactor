name := "akka-transactor"

version := "0.1"

scalaVersion := "2.12.8"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.google.guava" % "guava" % "27.1-jre",
  "org.apache.commons" % "commons-lang3" % "3.8.1",
  "com.typesafe.akka" %% "akka-actor" % "2.5.22",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.22" % Test
)