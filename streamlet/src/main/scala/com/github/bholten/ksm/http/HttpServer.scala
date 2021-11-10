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

package com.github.bholten.ksm.http

import cats.effect._
import com.github.bholten.ksm.streams.StreamletContext
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.{ HttpApp, HttpRoutes }

private[ksm] object HttpServer {
  val banner: List[String] =
    """
      |
      |  .-. .-')   .-')   _   .-')
      |  \  ( OO ) ( OO ).( '.( OO )_
      |  ,--. ,--.(_)---\_),--.   ,--.)
      |  |  .'   //    _ | |   `.'   |
      |  |      /,\  :` `. |         |
      |  |     ' _)'..`''.)|  |'.'|  |
      |  |  .   \ .-._)   \|  |   |  |
      |  |  |\   \\       /|  |   |  |
      |  `--' '--' `-----' `--'   `--'
      |Kafka Streams Microframework 0.0.1
      |
      |""".stripMargin.split("\n").toList

  case class Routes[F[_]: Concurrent](streams: StreamletContext) extends Http4sDsl[F] {
    val httpApp: HttpApp[F] = HttpRoutes
      .of[F] {
        case _ @GET -> Root              => Ok()
        case _ @GET -> Root / "health"   => if (streams.isHealthy) Ok() else InternalServerError()
        case _ @GET -> Root / "metrics"  => Ok(streams.metrics)
        case _ @GET -> Root / "topology" => Ok(streams.describe())
      }
      .orNotFound
  }
}
