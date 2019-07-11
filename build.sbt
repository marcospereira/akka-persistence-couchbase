import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys
import com.typesafe.sbt.SbtMultiJvm

inThisBuild(
  Seq(
    organization := "com.lightbend.akka",
    organizationName := "Lightbend Inc.",
    homepage := Some(url("https://doc.akka.io/docs/akka-persistence-couchbase/current")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/akka/akka-persistence-couchbase"),
              "https://github.com/akka/akka-persistence-couchbase.git")
    ),
    startYear := Some(2018),
    developers += Developer("contributors",
                            "Contributors",
                            "https://gitter.im/akka/dev",
                            url("https://github.com/akka/akka-persistence-couchbase/graphs/contributors")),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    description := "A replicated Akka Persistence journal backed by Couchbase",
  )
)

def common: Seq[Setting[_]] = Seq(
  crossScalaVersions := Seq(Dependencies.Scala213, Dependencies.Scala212),
  scalaVersion := Dependencies.Scala212,
  crossVersion := CrossVersion.binary,
  scalafmtOnCompile := true,
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8", // yes, this is 2 args
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Xfatal-warnings"
  ),
  bintrayOrganization := Some("akka"),
  bintrayPackage := "akka-persistence-couchbase",
  bintrayRepository := (if (isSnapshot.value) "snapshots" else "maven"),
  // Setting javac options in common allows IntelliJ IDEA to import them automatically
  javacOptions in compile ++= Seq(
    "-encoding",
    "UTF-8",
    "-source",
    "1.8",
    "-target",
    "1.8",
    "-parameters", // This param is required for Jackson serialization to preserve method parameter names
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  headerLicense := Some(
    HeaderLicense.Custom(
      """Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>"""
    )
  ),
  logBuffered in Test := System.getProperty("akka.logBufferedTests", "false").toBoolean,
  // show full stack traces and test case durations
  testOptions in Test += Tests.Argument("-oDF"),
  // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
  // -a Show stack traces and exception class name for AssertionErrors.
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
  // disable parallel tests
  parallelExecution in Test := false,
  fork := true
)

lazy val dontPublish = Seq(
  skip in publish := true,
  whitesourceIgnore := true,
  publishArtifact in Compile := false
)

def multiJvmTestSettings: Seq[Setting[_]] =
  SbtMultiJvm.multiJvmSettings ++ Seq(
    // `database.port` required for multi-dc tests that extend AbstractClusteredPersistentEntityConfig
    MultiJvmKeys.jvmOptions in MultiJvm := Seq("-Ddatabase.port=0")
  )

lazy val root = (project in file("."))
  .settings(common)
  .settings(dontPublish)
  .settings(
    name := "akka-persistence-couchbase-root",
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    // workaround for https://github.com/sbt/sbt/issues/3465
    crossScalaVersions := List(),
  )
  .aggregate((Seq(core, docs) ++ lagomModules).map(Project.projectToRef): _*)

lazy val core = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(AutomaticModuleName.settings("akka.persistence.couchbase"))
  .settings(common)
  .settings(
    name := "akka-persistence-couchbase",
    libraryDependencies := Dependencies.core
  )

lazy val lagomModules = Seq[Project](
  `lagom-persistence-couchbase-core`,
  `lagom-persistence-couchbase-javadsl`,
  `lagom-persistence-couchbase-scaladsl`
)

/**
 * This module contains copy-pasted parts from Lagom project that are not available outside of the project
 * because they are not published as part of the result artifacts.
 *
 * This module combines the reusable parts that reside in Lagom project in next modules:
 *
 * persistence/core
 * persistence/javadsl
 * persistence/scaladsl
 *
 * For simplicity sake here they are combined into one module.
 *
 * TODO: It can be removed once it's resolved (see https://github.com/lagom/lagom/issues/1634)
 */
lazy val `copy-of-lagom-persistence-test` =
  (project in file("lagom-persistence-couchbase/copy-of-lagom-persistence-test"))
    .settings(common)
    .settings(dontPublish)
    .settings(
      // This modules copy-pasted preserve it as is
      scalafmtOnCompile := false,
      libraryDependencies := Dependencies.`copy-of-lagom-persistence-test`,
    )

lazy val `lagom-persistence-couchbase-core` = (project in file("lagom-persistence-couchbase/core"))
  .dependsOn(core % "compile;test->test")
  .settings(common)
  .settings(AutomaticModuleName.settings("lagom.persistence.couchbase.core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "lagom-persistence-couchbase-core",
    libraryDependencies := Dependencies.`lagom-persistence-couchbase-core`,
  )

lazy val `lagom-persistence-couchbase-javadsl` = (project in file("lagom-persistence-couchbase/javadsl"))
  .settings(common)
  .settings(AutomaticModuleName.settings("lagom.persistence.couchbase.javadsl"))
  .dependsOn(
    core % "compile;test->test",
    `lagom-persistence-couchbase-core` % "compile;test->test",
    `copy-of-lagom-persistence-test` % "test->test"
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "lagom-javadsl-persistence-couchbase",
    libraryDependencies := Dependencies.`lagom-persistence-couchbase-javadsl`,
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .settings(multiJvmTestSettings: _*)

lazy val `lagom-persistence-couchbase-scaladsl` = (project in file("lagom-persistence-couchbase/scaladsl"))
  .dependsOn(
    core % "compile;test->test",
    `lagom-persistence-couchbase-core` % "compile;test->test",
    `copy-of-lagom-persistence-test` % "test->test"
  )
  .settings(common)
  .settings(AutomaticModuleName.settings("lagom.persistence.couchbase.scaladsl"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "lagom-scaladsl-persistence-couchbase",
    libraryDependencies := Dependencies.`lagom-persistence-couchbase-scaladsl`,
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .settings(multiJvmTestSettings: _*)

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(AkkaParadoxPlugin)
  .settings(common)
  .settings(dontPublish)
  .settings(
    name := "Akka Persistence Couchbase",
    crossScalaVersions := Seq(Dependencies.Scala212),
    paradoxGroups := Map("Language" -> Seq("Java", "Scala")),
    paradoxProperties ++= Map(
      "akka.version" -> Dependencies.AkkaVersion,
      "alpakkaCouchbase.version" -> Dependencies.AlpakkaCouchbaseVersion,
      "extref.akka-docs.base_url" -> s"https://doc.akka.io/docs/akka/${Dependencies.AkkaVersion}/%s",
      "extref.java-docs.base_url" -> "https://docs.oracle.com/en/java/javase/11/%s",
      "scaladoc.scala.base_url" -> s"https://www.scala-lang.org/api/current/",
      "scaladoc.akka.base_url" -> s"https://doc.akka.io/api/akka/${Dependencies.AkkaVersion}",
      "scaladoc.com.typesafe.config.base_url" -> s"https://lightbend.github.io/config/latest/api/"
    ),
    resolvers += Resolver.jcenterRepo
  )
  .dependsOn(`lagom-persistence-couchbase-scaladsl`, `lagom-persistence-couchbase-javadsl`)
