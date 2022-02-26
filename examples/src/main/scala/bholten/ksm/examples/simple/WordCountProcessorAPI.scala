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
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.api.{
  Processor,
  ProcessorContext,
  ProcessorSupplier,
  Record
}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.state.{ KeyValueStore, StoreBuilder, Stores }

// This is the Processor-API version of the WordCount example.
// It is considerably more complex because you have to build the
// Topology manually, including the repartitioning.
// However, it is left as an example to show that ksm can do the
// Processor API just fine.
object WordCountProcessorAPI extends Streamlet with LazyLogging {
  private val stringSerde: Serde[String] = Serdes.stringSerde
  private val longSerde: Serde[Long]     = Serdes.longSerde
  private val INPUT_SOURCE               = "input"
  private val REPARTITIONER_SINK         = "repartitioner-sink"
  private val REPARTITIONER_PROC         = "repartitioner"
  private val REPARTITIONER_SOURCE       = "repartitioner-source"
  private val WORD_COUNT_PROC            = "wordcount"
  private val WORD_COUNT_STORE           = "wordcount-store"
  private val WORD_COUNT_SINK            = "wordcount-sink"

  // ksm doesn't care how you build your topology, just that you build it.
  // The topology is built manually in the Processor API.
  logger.info("Starting WordCount, processor API version")
  private val config              = ConfigFactory.load()
  override val topology: Topology = generateTopology(config)

  // We pull the topology into a function to make it easier to test
  def generateTopology(config: Config): Topology = {
    val inputTopic  = config.getString("word-count.input.topic")
    val outputTopic = config.getString("word-count.output.topic")
    // You can pull out any of the Streamlet configuration if you need,
    // e.g. getting the application ID to construct a repartition topic
    val repartitionTopic =
      s"repartition-${config.getString("streamlet.kafka.streams.application.id")}"

    val topology = new Topology
    topology.addSource(
      INPUT_SOURCE,
      stringSerde.deserializer(),
      stringSerde.deserializer(),
      inputTopic
    )
    topology.addProcessor(REPARTITIONER_PROC, repartitionerSupplier, INPUT_SOURCE)
    topology.addSink(
      REPARTITIONER_SINK,
      repartitionTopic,
      stringSerde.serializer(),
      stringSerde.serializer(),
      REPARTITIONER_PROC
    )
    topology.addSource(
      REPARTITIONER_SOURCE,
      stringSerde.deserializer(),
      stringSerde.deserializer(),
      repartitionTopic
    )
    topology.addProcessor(WORD_COUNT_PROC, wordCountSupplier, REPARTITIONER_SOURCE)
    topology.addStateStore(stateStoreBuilder, WORD_COUNT_PROC)
    topology.addSink(
      WORD_COUNT_SINK,
      outputTopic,
      stringSerde.serializer(),
      longSerde.serializer(),
      WORD_COUNT_PROC
    )
  }

  private def repartitionerSupplier: ProcessorSupplier[String, String, String, String] = () =>
    new WordCountRepartitioner
  private def wordCountSupplier: ProcessorSupplier[String, String, String, Long] = () =>
    new WordCountProcessorAPI

  private def stateStoreBuilder: StoreBuilder[KeyValueStore[String, Long]] =
    Stores.keyValueStoreBuilder(
      Stores.inMemoryKeyValueStore(WORD_COUNT_STORE),
      stringSerde,
      longSerde
    )

  // You need to manually repartition the words, so you need an extra processor
  class WordCountRepartitioner extends Processor[String, String, String, String] with LazyLogging {
    private var context: ProcessorContext[String, String] = _

    override def init(context: ProcessorContext[String, String]): Unit =
      this.context = context

    override def process(record: Record[String, String]): Unit =
      Option(record.value())
        .foreach { sentence =>
          sentence
            .split("\\W+")
            .foreach { word =>
              context.forward(new Record(record.key, word, record.timestamp()), REPARTITIONER_SINK)
            }
        }

    override def close(): Unit =
      logger.warn(s"Closing WordCountRepartitioner processor ${context.taskId()}")
  }
}

class WordCountProcessorAPI extends Processor[String, String, String, Long] with LazyLogging {
  import WordCountProcessorAPI._
  private var context: ProcessorContext[String, Long]     = _
  private var wordCountStore: KeyValueStore[String, Long] = _

  override def init(context: ProcessorContext[String, Long]): Unit = {
    this.context = context
    this.wordCountStore = context
      .getStateStore(WORD_COUNT_STORE)
      .asInstanceOf[KeyValueStore[String, Long]]
  }

  override def process(record: Record[String, String]): Unit =
    Option(record.value)
      .foreach { word =>
        Option(wordCountStore.get(word))
          .map(_ + 1L)
          .orElse(Some(1L))
          .foreach { wordCount =>
            wordCountStore.put(word, wordCount)
            context.forward(new Record(word, wordCount, record.timestamp()), WORD_COUNT_SINK)
          }
      }

  override def close(): Unit =
    logger.warn(s"Closing WordCount processor ${context.taskId()}")
}
