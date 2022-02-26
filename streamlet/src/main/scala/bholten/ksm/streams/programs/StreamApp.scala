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

package bholten.ksm.streams.programs

import bholten.ksm.config.StreamletConfig
import bholten.ksm.http.api.KafkaRoutes
import bholten.ksm.streams.api.StartupHandler
import bholten.ksm.streams.resources.{ HttpServer, StreamContext }
import cats.effect.kernel.Async
import cats.implicits._
import org.apache.kafka.streams.Topology

object StreamApp {
  def runF[F[_]: Async](
      topology: Topology,
      kafkaRoutes: KafkaRoutes[F],
      beforeStart: StartupHandler
  ): F[Nothing] =
    StreamletConfig
      .loadDefault[F]
      .flatMap[Nothing] { config =>
        val context = StreamContext(topology, config.kafkaProperties, beforeStart)
        HttpServer.resource(config, kafkaRoutes, context).useForever
      }
}
