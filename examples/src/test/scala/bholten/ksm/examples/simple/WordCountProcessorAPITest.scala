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

import com.typesafe.config.ConfigFactory
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.{ TestInputTopic, TestOutputTopic, TopologyTestDriver }
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.jdk.CollectionConverters._

class WordCountProcessorAPITest extends AnyFlatSpec with Matchers with BeforeAndAfter {
  var testDriver: TopologyTestDriver             = _
  var inputTopic: TestInputTopic[String, String] = _
  var outputTopic: TestOutputTopic[String, Long] = _

  after {
    testDriver.close()
  }

  before {
    val config = ConfigFactory
      .parseString(
        s"""
           |word-count.input.topic = "test-input"
           |word-count.output.topic = "test-output"
           |streamlet.kafka.streams.application.id = "test-word-count-${UUID.randomUUID()}"
           |""".stripMargin
      )
      .withFallback(ConfigFactory.load())

    val topology = WordCountProcessorAPI.generateTopology(config)
    testDriver = new TopologyTestDriver(topology)
    inputTopic = testDriver.createInputTopic(
      "test-input",
      Serdes.stringSerde.serializer(),
      Serdes.stringSerde.serializer()
    )
    outputTopic = testDriver.createOutputTopic(
      "test-output",
      Serdes.stringSerde.deserializer(),
      Serdes.longSerde.deserializer()
    )
  }

  it should "count the number of words" in {
    List(
      "test",
      "this is the test",
      "the quick brown fox"
    ).foreach { sentence =>
      inputTopic.pipeInput(UUID.randomUUID().toString, sentence)
    }

    val results = outputTopic.readKeyValuesToMap().asScala

    results.size shouldBe 7
    results("test") shouldBe 2
    results("the") shouldBe 2
    results("is") shouldBe 1
    results("this") shouldBe 1
    results("quick") shouldBe 1
    results("brown") shouldBe 1
    results("fox") shouldBe 1
  }
}
