import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Comment, Elem, Node => XmlNode, NodeSeq => XmlNodeSeq}

import UpdateReadme._

trait BuildSettings {
  val crossScalaV = Seq("2.10.6", "2.11.8")
  val scalaV = crossScalaV.last
  val paradiseV = "2.1.0"
  val jacksonV = "2.7.2"

  val buildSettings: Seq[Def.Setting[_]] = Defaults.coreDefaultSettings ++ Seq(
    initialize := {
      // require JDK 1.7 for development
      // will need to use both JDK 1.7 and Scala 2.12/JDK 1.8 to cross-build for Scala 2.12
      val _ = initialize.value // run the previous initialization
      val required = "1.7"
      val current  = sys.props("java.specification.version")
      assert(current == required, s"JDK $required is required for compatibility; current version = $current")
    },
    organization := "com.kakao.mango",
    isSnapshot := version.value.endsWith("-SNAPSHOT"),
    scalaVersion := scalaV,
    crossScalaVersions := crossScalaV,
    compileOrder := CompileOrder.Mixed,
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseV cross CrossVersion.full),
    scalacOptions := Seq("-feature", "-unchecked", "-encoding", "utf8"),
    javacOptions := Seq("-XDignore.symbol.file"),
    sources in (Compile, doc) := Seq.empty,
    publishTo := {
      val sonatype = "https://oss.sonatype.org"
      if (isSnapshot.value)
        Some("Sonatype Snapshots" at s"$sonatype/content/repositories/snapshots")
      else
        Some("Sonatype Staging" at s"$sonatype/service/local/staging/deploy/maven2")
    },
    // use local maven repository
    resolvers += Resolver.mavenLocal,
    // update README.md automatically on release
    releaseProcess := customReleaseProcess,
    releaseCrossBuild := true,
    // test dependencies
    libraryDependencies ++= Seq(
      "org.mockito" % "mockito-core" % "1.9.5",
      "log4j" % "log4j" % "1.2.17",
      "org.scalatest" %% "scalatest" % "3.0.1",
      "org.slf4j" % "slf4j-log4j12" % "1.7.5"
    ).map(_ % "test"),
    pomPostProcess := { (node: XmlNode) =>
      // remove provided/test dependencies from the resulting POM, to speed up the resolving process
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case e: Elem if e.label == "dependency" && e.child.exists(c => c.label == "scope" && Seq("provided", "test").contains(c.text)) =>
            val organization = e.child.filter(_.label == "groupId").flatMap(_.text).mkString
            val artifact = e.child.filter(_.label == "artifactId").flatMap(_.text).mkString
            val version = e.child.filter(_.label == "version").flatMap(_.text).mkString
            val scope = e.child.filter(_.label == "scope").flatMap(_.text).mkString
            Comment(s"$scope dependency $organization#$artifact;$version has been omitted")
          case _ => node
        }
      }).transform(node).head
    }
  ) ++ {
    // add Sonatype credentials only when the file exists
    val path = Path.userHome / ".ivy2" / ".credentials"
    if (path.exists())
      credentials += Credentials(path)
    else
      Nil
  }

  def dep(modules: ModuleID*) = libraryDependencies ++= modules

  val shades = dep(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonV,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonV,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonV,
    "com.fasterxml.jackson.module" % "jackson-module-afterburner" % jacksonV,
    "com.fasterxml.jackson.module" % "jackson-module-paranamer" % jacksonV,
    "com.ning" % "async-http-client" % "1.9.29",
    ("org.hbase" % "asynchbase" % "1.7.2")
      .exclude("io.netty", "netty")         // use the Netty version
      .exclude("org.jboss.netty", "netty")  // this pulls Netty 3.2 which is too old
      .exclude("org.slf4j", "*"),           // prevents importing any SLF4j bridges
    "com.google.guava" % "guava" % "18.0"   // note that this version is being shaded, and Guava 14.0.1 will be forced in other subprojects
  )

  val curator = dep("org.apache.curator" % "curator-framework" % "2.11.0")
  val couchbase = dep(
    "com.couchbase.client" % "java-client" % "2.4.1"
  )

  val hbaseTest = dep(Seq("server", "common", "hadoop-compat", "hadoop2-compat").map { lib =>
    "org.apache.hbase" % s"hbase-$lib" % "1.2.4" % "test"
  }: _*) ++ dep(
    "org.apache.hadoop" % "hadoop-hdfs" % "2.5.1" % "test",
    "com.google.guava" % "guava" % "14.0.1" % "test" force()
  )

  val config = dep("com.typesafe" % "config" % "1.2.1")
  val rx = dep("io.reactivex" %% "rxscala" % "0.26.5")
  val slf4j = dep("org.slf4j" % "slf4j-api" % "1.7.21")

  val loggingProvided = slf4j ++ dep(
    // used by LoggingUtils; set as "provided", since Mango can be used in either logback- or log4j-based deployment environments
    "log4j" % "log4j" % "1.2.17" % "provided",
    "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "provided",
    "ch.qos.logback" % "logback-classic" % "1.1.7" % "provided"
  )
  val macros = dep("org.scalamacros" %% "paradise" % paradiseV cross CrossVersion.full)
  val reflect = libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

  val bouncycastle = dep("org.bouncycastle" % "bcprov-jdk15on" % "1.50")
}

object Build extends Build with BuildSettings {

  lazy val mango = Project(
    id = "mango",
    base = file("."),
    settings = buildSettings ++ Seq(TaskKey[Unit]("update-readme") := updateReadme(null))
  ).dependsOn(
    mangoShaded, mangoCore, mangoMacro, mangoZk,
    mangoHBase, mangoCouchbase, mangoElasticSearch
  ).aggregate(
    mangoShaded, mangoCore, mangoMacro, mangoZk,
    mangoHBase, mangoCouchbase, mangoElasticSearch
  )

  // this is where package relocation happens, but is not part of the parent project
  // the resulting shaded jar will be picked up and used as a artifact of mango-shaded project
  lazy val mangoShadedLib = Project(
    id = "mango-shaded-lib",
    base = file("mango-shaded-lib"),
    settings = buildSettings ++ slf4j ++ shades ++ Seq(
      assemblyOutputPath in assembly := crossTarget.value / "mango-shaded.jar",
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
      assemblyShadeRules in assembly := Seq(
        ShadeRule.rename("com.fasterxml.jackson.**" -> "com.kakao.shaded.jackson.@1").inAll,
        ShadeRule.rename("com.google.common.**" -> "com.kakao.shaded.guava.@1").inAll,
        ShadeRule.rename("com.ning.**" -> "com.kakao.shaded.ning.@1").inAll,
        ShadeRule.rename("com.thoughtworks.paranamer.**" -> "com.kakao.shaded.paranamer.@1").inAll,
        ShadeRule.rename("org.jboss.netty.**" -> "com.kakao.shaded.netty.@1").inAll,
        ShadeRule.rename("org.hbase.**" -> "com.kakao.shaded.hbase.@1").inAll
      )
    )
  )

  lazy val mangoShadedLibRef = LocalProject("mango-shaded-lib")

  lazy val mangoShaded = Project(
    id = "mango-shaded",
    base = file("mango-shaded"),
    settings = buildSettings ++ Seq(
      compile in Compile <<= (compile in Compile) dependsOn (assembly in mangoShadedLibRef),
      assemblyOutputPath in assembly := crossTarget.value / (name.value + "_" + scalaBinaryVersion.value + "-" + version.value + ".jar"),
      TaskKey[File]("package", s"skip package task") := crossTarget.value / (name.value + "_" + scalaBinaryVersion.value + "-" + version.value + ".jar"),
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
      unmanagedJars in Compile := Seq(Attributed.blank((crossTarget in mangoShadedLibRef).value / "mango-shaded.jar"))
    ) ++ addArtifact(artifact in (Compile, assembly), assembly) ++ Seq(
      artifacts := artifacts.value.filterNot(_.configurations.toSeq.exists(_.name == "compile")),
      packagedArtifacts := packagedArtifacts.value.filterKeys(!_.configurations.toSeq.exists(_.name == "compile"))
    )
  )

  lazy val mangoCore = Project(
    id = "mango-core",
    base = file("mango-core"),
    settings = buildSettings ++ config ++ rx ++ reflect ++ bouncycastle ++ loggingProvided
  ).dependsOn(mangoShaded, mangoMacro)

  lazy val mangoMacro = Project(
    id = "mango-macro",
    base = file("mango-macro"),
    settings = buildSettings ++ macros
  )

  lazy val mangoZk = Project(
    id = "mango-zk",
    base = file("mango-zk"),
    settings = buildSettings ++ curator
  ).dependsOn(mangoCore % "compile->compile;test->test")

  lazy val mangoHBase = Project(
    id = "mango-hbase",
    base = file("mango-hbase"),
    settings = buildSettings ++ hbaseTest
  ).dependsOn(mangoZk % "compile->compile;test->test")

  lazy val mangoCouchbase = Project(
    id = "mango-couchbase",
    base = file("mango-couchbase"),
    settings = buildSettings ++ couchbase ++ Seq(
      unmanagedJars in Test += file(baseDirectory.value + "/lib/CouchbaseMock-1.4.4.jar")
    )
  ).dependsOn(mangoCore % "compile->compile;test->test")

  lazy val mangoElasticSearch = Project(
    id = "mango-elasticsearch",
    base = file("mango-elasticsearch"),
    settings = buildSettings
  ).dependsOn(mangoCore % "compile->compile;test->test")

}
