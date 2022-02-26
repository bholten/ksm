// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization := "bholten",
    organizationName := "Brennan Holten",
    startYear := Some(2022),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := "2.13.5",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-encoding",
      "UTF-8",
      "-Ywarn-unused:imports",
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalafmtOnCompile := true,
    dynverSeparator := "_", // the default `+` is not compatible with docker tags
    parallelExecution := false
  )
)

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val ksm =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .aggregate(
      examples,
      streamlet
    )

lazy val examples =
  project
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      name := "examples"
    )
    .settings(
      libraryDependencies ++= Seq(
        library.scalaLogging,
        library.logback,
        library.scalaTest        % Test,
        library.kafkaStreamsTest % Test
      )
    )
    .dependsOn(streamlet)

lazy val streamlet =
  project
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      name := "streamlet"
    )
    .settings(
      libraryDependencies ++= Seq(
        library.kafka,
        library.kafkaStreams,
        library.config,
        library.catsEffect,
        library.statsD,
        library.httpsDsl,
        library.httpsCore,
        library.httpsCirce,
        library.httpsServer,
        library.httpsEmberServer,
        library.circeCore,
        library.circeParser,
        library.circeGeneric,
        library.scalaTest           % Test,
        library.logback             % Test,
        library.embeddedKafka       % Test,
        library.munit               % Test,
        library.munitScalaCheck     % Test,
        library.munitCatsEffect     % Test,
        library.weaverKats          % Test,
        library.scalaTestCatsEffect % Test
      )
    )
    .settings(
      testFrameworks += new TestFramework("munit.Framework")
    )

// *****************************************************************************
// Project settings
// *****************************************************************************

lazy val commonSettings =
  Seq(
    // Also (automatically) format build definition together with sources
    Compile / scalafmt := {
      val _ = (Compile / scalafmtSbt).value
      (Compile / scalafmt).value
    },
  )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val munit           = "0.7.19"
      val munitCatsEffect = "1.0.7"
      val scalaLogging    = "3.9.2"
      val logback         = "1.1.7"
      val config          = "1.4.1"
      val kafka           = "2.8.1"
      val circe           = "0.14.1"
      val catsEffect      = "3.3.5"
      val http4s          = "0.23.9"
      val statsD          = "2.10.5"
    }
    val scalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"         % Version.scalaLogging
    val logback          = "ch.qos.logback"              % "logback-classic"       % Version.logback
    val config           = "com.typesafe"                % "config"                % Version.config
    val catsEffect       = "org.typelevel"              %% "cats-effect"           % Version.catsEffect
    val circeCore        = "io.circe"                   %% "circe-core"            % Version.circe
    val circeGeneric     = "io.circe"                   %% "circe-generic"         % Version.circe
    val circeParser      = "io.circe"                   %% "circe-parser"          % Version.circe
    val kafka            = "org.apache.kafka"           %% "kafka"                 % Version.kafka
    val kafkaStreams     = "org.apache.kafka"           %% "kafka-streams-scala"   % Version.kafka
    val httpsDsl         = "org.http4s"                 %% "http4s-dsl"            % Version.http4s
    val httpsEmberServer = "org.http4s"                 %% "http4s-ember-server"   % Version.http4s
    val httpsCirce       = "org.http4s"                 %% "http4s-circe"          % Version.http4s
    val httpsCore        = "org.http4s"                 %% "http4s-core"           % Version.http4s
    val httpsServer      = "org.http4s"                 %% "http4s-server"         % Version.http4s
    val statsD           = "com.datadoghq"               % "java-dogstatsd-client" % Version.statsD
    // Testing
    val munit               = "org.scalameta"           %% "munit"                         % Version.munit
    val munitScalaCheck     = "org.scalameta"           %% "munit-scalacheck"              % Version.munit
    val munitCatsEffect     = "org.typelevel"           %% "munit-cats-effect-3"           % Version.munitCatsEffect
    val scalaTest           = "org.scalatest"           %% "scalatest"                     % "3.1.2"
    val scalaTestCatsEffect = "org.typelevel"           %% "cats-effect-testing-scalatest" % "1.4.0"
    val kafkaStreamsTest    = "org.apache.kafka"         % "kafka-streams-test-utils"      % Version.kafka
    val embeddedKafka       = "io.github.embeddedkafka" %% "embedded-kafka"                % "2.8.1"
    val weaverKats          = "com.disneystreaming"     %% "weaver-cats"                   % "0.6.9"
  }
