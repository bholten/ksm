/*
 * Copyright 2021 Brennan Holten
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

package com.github.bholten.ksm.streams

import cats.effect.concurrent.Deferred
import cats.effect.{ Async, Concurrent, Resource, Sync }
import cats.implicits._
import com.github.bholten.ksm.config.StreamletConfig
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.{ KafkaStreams, Topology }

import scala.jdk.CollectionConverters._

private[ksm] final case class StreamletContext(
    topology: Topology,
    streamletConfig: StreamletConfig
) extends LazyLogging {
  private val kafkaStreams: KafkaStreams =
    new KafkaStreams(topology, streamletConfig.kafkaProperties)
  private val startupHandler: StreamsStartupHandler = streamletConfig.startUpHandler

  def isHealthy: Boolean =
    kafkaStreams.state().isRunningOrRebalancing

  def describe(): String = topology.describe().toString

  def metrics: LazyList[(String, Double)] =
    kafkaStreams
      .metrics()
      .asScala
      .to(LazyList)
      .map(e => e._1.toString -> e._2.value())

  def run[F[_]: Concurrent]: F[KafkaStreams] =
    for {
      signal <- Deferred[F, KafkaStreams]
      resource <-
        Resource
          .make {
            Sync[F].delay(kafkaStreams)
          } { streams =>
            Sync[F]
              .delay(streams.close(streamletConfig.startupTimeoutDuration))
              .flatMap(_ => signal.complete(streams))
          }
          .use { streams =>
            Async[F].async { cb =>
              startupHandler.beforeStart(streams, cb)
            }
          }
          .redeemWith(_ => signal.get, _ => signal.get)
    } yield resource
}
