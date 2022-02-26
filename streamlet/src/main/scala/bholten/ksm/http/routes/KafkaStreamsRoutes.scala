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

package bholten.ksm.http.routes

import bholten.ksm.http.api.KafkaRoutes
import bholten.ksm.streams.api.StreamHandler
import cats.effect.Concurrent
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._

case class KafkaStreamsRoutes[F[_]: Concurrent]() extends Http4sDsl[F] with KafkaRoutes[F] {
  import cats.syntax.flatMap._

  override def routes(handler: StreamHandler[F]): HttpRoutes[F] =
    HttpRoutes
      .of[F] {
        case _ @GET -> Root => Ok()
        case _ @GET -> Root / "health" =>
          handler.isHealthy.flatMap(healthy => if (healthy) Ok() else InternalServerError())
        case _ @GET -> Root / "metrics"  => Ok(handler.metrics())
        case _ @GET -> Root / "topology" => Ok(handler.describe())
      }
}
