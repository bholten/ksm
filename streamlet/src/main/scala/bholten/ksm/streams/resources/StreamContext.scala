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

package bholten.ksm.streams.resources

import bholten.ksm.streams.api.{ Context, StartupHandler, StreamHandler }
import cats.implicits._
import cats.effect.kernel.{ Resource, Sync }
import org.apache.kafka.streams.{ KafkaStreams, Topology }

import java.util.Properties
import scala.jdk.CollectionConverters._

case class StreamContext[F[_]: Sync](
    topology: Topology,
    properties: Properties,
    beforeStart: StartupHandler
) extends Context[F] {
  val resource: Resource[F, StreamHandler[F]] =
    Resource
      .make {
        Sync[F]
          .delay {
            val kafkaStreams = new KafkaStreams(topology, properties)

            new StreamHandler[F] {
              override def start(): F[Unit] =
                Sync[F].blocking(kafkaStreams.start())

              override def stop(): F[Unit] =
                Sync[F].blocking(kafkaStreams.close())

              override def isHealthy: F[Boolean] =
                Sync[F].delay(kafkaStreams.state().isRunningOrRebalancing)

              override def metrics(): F[LazyList[(String, Double)]] =
                Sync[F].delay(
                  kafkaStreams
                    .metrics()
                    .asScala
                    .to(LazyList)
                    .map { e =>
                      val metric =
                        if (e._2.metricValue().isInstanceOf[Double])
                          Double.unbox(e._2)
                        else 0.0
                      e._1.toString -> metric
                    }
                )

              override def describe(): F[String] =
                Sync[F].delay(topology.describe().toString)
            }
          }
          .flatTap { handler =>
            handler.start()
          }
      } { handler =>
        handler.stop()
      }
}
