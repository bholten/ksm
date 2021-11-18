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

package com.github.bholten.ksm

import cats.effect.{ ExitCode, IO, IOApp, Resource, SyncIO }
import com.github.bholten.ksm.config.StreamletConfig
import com.github.bholten.ksm.http.HttpServer
import com.github.bholten.ksm.streams.StreamletContext
import org.apache.kafka.streams.Topology
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

trait Streamlet extends IOApp.WithContext {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] =
    Resource.liftF(SyncIO(ec))
  val topology: Topology

  override def run(args: List[String]): IO[ExitCode] =
    for {
      conf <- StreamletConfig.loadDefault
      streams = StreamletContext(topology, conf)
      _ <- streams.run[IO].start
      server <-
        BlazeServerBuilder[IO](ec)
          .bindHttp(conf.httpConfig.port, conf.httpConfig.host)
          .withHttpApp(HttpServer.Routes[IO](streams).httpApp)
          .withBanner(HttpServer.banner)
          .resource
          .use(_ => IO.never)
          .as(ExitCode.Success)
    } yield server
}
