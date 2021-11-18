// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization := "com.github.bholten.ksm",
    organizationName := "Brennan Holten",
    startYear := Some(2021),
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

lazy val serdes =
  project
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      name := "serdes",
      resolvers += "confluent" at "https://packages.confluent.io/maven/"
    )
    .settings(
      libraryDependencies ++= Seq(
        library.circeCore,
        library.circeParser,
        library.circeGeneric,
        library.circeOptics,
        library.confluentSchemaRegistryClient,
        library.kafka
      )
    )

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
        library.cats,
        library.catsEffect,
        library.statsD,
        library.httpsDsl,
        library.httpsCore,
        library.httpsCirce,
        library.httpsServer,
        library.httpsBlazeServer,
        library.circeCore,
        library.circeParser,
        library.circeGeneric,
        library.scalaTest       % Test,
        library.catsEffectLaws  % Test,
        library.embeddedKafka   % Test,
        library.munit           % Test,
        library.munitScalaCheck % Test,
      )
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
      val munit        = "0.7.19"
      val scalaLogging = "3.9.2"
      val logback      = "1.1.7"
      val config       = "1.4.1"
      val kafka        = "2.8.1"
      val circe        = "0.13.0"
      val cats         = "2.3.1"
      val statsD       = "2.10.5"
      val confluent    = "6.2.1"
    }
    val munit           = "org.scalameta" %% "munit"            % Version.munit
    val munitScalaCheck = "org.scalameta" %% "munit-scalacheck" % Version.munit

    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"   % Version.scalaLogging
    val logback      = "ch.qos.logback"              % "logback-classic" % Version.logback
    val config       = "com.typesafe"                % "config"          % Version.config

    // Cats
    val cats       = "org.typelevel" %% "cats-core"   % Version.cats
    val catsEffect = "org.typelevel" %% "cats-effect" % Version.cats

    // Circe (JSON)
    val circeCore    = "io.circe" %% "circe-core"    % Version.circe
    val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
    val circeParser  = "io.circe" %% "circe-parser"  % Version.circe
    val circeOptics  = "io.circe" %% "circe-optics"  % Version.circe

    // Confluent
    val confluentSchemaRegistryClient =
      "io.confluent" % "kafka-schema-registry-client" % Version.confluent

    // Kafka
    val kafka        = "org.apache.kafka" %% "kafka"               % Version.kafka
    val kafkaStreams = "org.apache.kafka" %% "kafka-streams-scala" % Version.kafka

    // http4s
    val httpsDsl         = "org.http4s" %% "http4s-dsl"          % "0.21.15"
    val httpsBlazeServer = "org.http4s" %% "http4s-blaze-server" % "0.21.15"
    val httpsCirce       = "org.http4s" %% "http4s-circe"        % "0.21.15"
    val httpsCore        = "org.http4s" %% "http4s-core"         % "0.21.15"
    val httpsServer      = "org.http4s" %% "http4s-server"       % "0.21.15"

    // StatsD
    val statsD = "com.datadoghq" % "java-dogstatsd-client" % Version.statsD

    // Testing
    val scalaTest        = "org.scalatest"           %% "scalatest"                % "3.1.2"
    val kafkaStreamsTest = "org.apache.kafka"         % "kafka-streams-test-utils" % Version.kafka
    val scalaCheck       = "org.scalacheck"          %% "scalacheck"               % "1.14.3"
    val embeddedKafka    = "io.github.embeddedkafka" %% "embedded-kafka"           % "2.8.1" // TODO
    val catsEffectLaws   = "org.typelevel"           %% "cats-effect-laws"         % Version.cats
  }
