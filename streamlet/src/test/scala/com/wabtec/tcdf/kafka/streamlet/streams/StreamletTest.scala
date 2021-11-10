package com.wabtec.tcdf.kafka.streamlet.streams

import cats.effect.IO._
import cats.effect._
import cats.effect.laws.util.TestContext
import com.github.bholten.ksm.Streamlet
import com.github.bholten.ksm.config.StreamletConfig
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.KTable
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class StreamletTest extends AnyFlatSpec with Matchers with BeforeAndAfter with EmbeddedKafka {
  // Random topology, does not really matter for this test as long as it is valid
  val wordCountTopology: Topology = {
    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.Serdes._

    val builder = new StreamsBuilder()
    val wordCounts: KTable[String, Long] =
      builder
        .stream[String, String]("input.topic")
        .flatMapValues(_.toLowerCase.split(" "))
        .selectKey { (_, word) =>
          word
        }
        .groupByKey
        .count()

    wordCounts.toStream.to("output.topic")
    val props = StreamletConfig.loadDefault.unsafeRunSync().kafkaProperties
    builder.build(props)
  }

  // TODO(brennan) make a better test
  it should "start the application" in {
    implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9092)

    val executionContext = TestContext.apply()

    // Applications are just values
    val application: Streamlet = new Streamlet {
      override implicit val ec: ExecutionContext = executionContext
      override val topology: Topology            = wordCountTopology
    }

    withRunningKafka {
      createCustomTopic("input.topic", scala.collection.immutable.Map[String, String](), 1, 1)
      createCustomTopic("output.topic", scala.collection.immutable.Map[String, String](), 1, 1)

      implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)
      implicit val timer: Timer[IO]     = IO.timer(executionContext)
      val test = for {
        fiber <- Concurrent[IO].start(application.run(List()))
        _     <- fiber.cancel
      } yield true

      test.unsafeRunTimed(10.seconds).get shouldBe true
    }
  }
}
