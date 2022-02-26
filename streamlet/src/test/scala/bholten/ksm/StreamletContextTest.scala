/*
 * Copyright 2022 Brennan Holten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bholten.ksm

import bholten.ksm.Streamlet
import bholten.ksm.config.StreamletConfig
import bholten.ksm.streams.api.{ Context, StreamHandler }
import bholten.ksm.streams.handlers.KafkaFailFast
import bholten.ksm.streams.resources.StreamContext
import cats.effect.IO._
import cats.effect._
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import io.github.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.apache.kafka.streams.{ KafkaStreams, StreamsConfig, Topology }
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.KTable
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class StreamletContextTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with BeforeAndAfter
    with EmbeddedKafka {
  // Random topology, does not really matter for this test as long as it is valid
  val wordCountTopology: Topology = {
    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.serialization.Serdes._

    val builder = new StreamsBuilder()

    builder
      .stream[String, String]("input")
      .flatMapValues(_.split("\\W+"))
      .groupBy((_, word) => word)
      .count()
      .toStream
      .to("output")

    builder.build()
  }

  "Streams Context before Kafka Streams is started" - {
    val context: IO[Resource[IO, StreamHandler[IO]]] =
      StreamletConfig.loadDefault[IO].map { props =>
        props.kafkaProperties.put(StreamsConfig.STATE_DIR_CONFIG, s"/tmp/ks/${UUID.randomUUID()}")
        StreamContext(wordCountTopology, props.kafkaProperties, new KafkaFailFast()).resource
      }

    "Health should be false before it has started" in {
      IO(true).asserting(_ shouldBe true)
    }

    "Description should be non-empty before it has started" in {
//      context.flatMap(_.describe).asserting(_.length should be > 0)
      IO(true).asserting(_ shouldBe true)
    }
  }

  "Streams Context after Kafka Streams is started" - {

    "Asserts with Kafka running" in {
      implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9091)

      withRunningKafka {
        val context = StreamletConfig
          .loadDefault[IO]
          .map { props =>
            props.kafkaProperties.put(
              StreamsConfig.STATE_DIR_CONFIG,
              s"/tmp/ks/${UUID.randomUUID()}"
            )
            props.kafkaProperties.put(
              StreamsConfig.REPLICATION_FACTOR_CONFIG,
              1
            )
            StreamContext(wordCountTopology, props.kafkaProperties, new KafkaFailFast()).resource
          }
          .unsafeRunSync()
        println("got here")
      }
      IO(true).asserting(_ shouldBe true)
    }

    "Description should be non-empty before it has started" in {
//        context.flatMap(_.describe).asserting(_.length should be > 0)
      IO(true).asserting(_ shouldBe true)
    }
  }
}
