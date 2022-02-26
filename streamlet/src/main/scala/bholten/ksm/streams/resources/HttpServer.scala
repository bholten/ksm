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

import bholten.ksm.config.StreamletConfig
import bholten.ksm.http.api.KafkaRoutes
import bholten.ksm.streams.api.Context
import cats.effect.kernel.{ Async, Resource }
import com.comcast.ip4s.{ Host, Port }
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object HttpServer {
  def resource[F[_]: Async](
      streamletConfig: StreamletConfig,
      kafkaRoutes: KafkaRoutes[F],
      context: Context[F]
  ): Resource[F, Server] =
    for {
      handler <- context.resource
      server <- EmberServerBuilder
        .default[F]
        .withHttpApp(kafkaRoutes.routes(handler).orNotFound)
        // TODO handle these later
        .withHost(Host.fromString(streamletConfig.httpConfig.host).get)
        .withPort(Port.fromInt(streamletConfig.httpConfig.port).get)
        .build
    } yield server
}
