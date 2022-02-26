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

package bholten.ksm.examples.simple

import bholten.ksm.Streamlet
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.Materialized
import org.apache.kafka.streams.state.internals.InMemoryKeyValueStore

// Streamlet gives you a main entrypoint automatically
object WordCount extends Streamlet with LazyLogging {
  logger.info("Starting WordCount")
  private val config = ConfigFactory.load()

  // The only thing you need to override is the Topology
  override val topology: Topology = generateTopology(config)

  // Split out the topology for easier testing, see test/ folder
  def generateTopology(config: Config): Topology = {
    // Scala gets extra help with implicits
    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.serialization.Serdes._
    implicit val countStore: Materialized[String, String, InMemoryKeyValueStore] =
      Materialized.as("count-store")

    val inputTopic  = config.getString("word-count.input.topic")
    val outputTopic = config.getString("word-count.output.topic")
    val builder     = new StreamsBuilder()

    // Get the input stream
    builder
      .stream[String, String](inputTopic)
      // Split the words
      .flatMapValues(_.split("\\W+"))
      // Repartition on word
      .groupBy((_, word) => word)
      // Aggregate => (word, count)
      .count()
      // Output to KStream
      .toStream
      .to(outputTopic)

    builder.build()
  }
  // That's it!
}
