name := "BlockforceImporter"

version := "0.1.0"

scalaVersion := "2.12.6"

resolvers += "micronautics/scala on bintray" at "http://dl.bintray.com/micronautics/scala"
resolvers += "ethereum/maven on bintray" at "https://dl.bintray.com/ethereum/maven/"
resolvers += "Maven" at "https://repo1.maven.org/maven2/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.3", 
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",  
  "org.json4s" %% "json4s-native" % "3.5.3",
  "com.micronautics" %% "web3j-scala" % "0.2.2",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.mongodb" %% "casbah" % "3.1.1"
)
