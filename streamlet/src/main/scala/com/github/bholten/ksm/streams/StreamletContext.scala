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

private[ksm] object StreamletContext extends LazyLogging {
  def run[F[_]: Concurrent](streamletContext: StreamletContext): F[KafkaStreams] =
    for {
      d <- Deferred[F, KafkaStreams]
      r <-
        Resource
          .make(
            Sync[F].delay(streamletContext.kafkaStreams)
          )(s =>
            Sync[F]
              .delay(s.close(streamletContext.streamletConfig.startupTimeoutDuration))
              .flatMap(_ => d.complete(s))
          )
          .use { streams =>
            Async[F].async { (k: Either[Throwable, Unit] => Unit) =>
              streamletContext.startupHandler.beforeStart(streams, k)
            }
          }
          .redeemWith(_ => d.get, _ => d.get)
    } yield r
}

private[ksm] final case class StreamletContext(
    topology: Topology,
    streamletConfig: StreamletConfig
) {
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
}
