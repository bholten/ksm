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

package bholten.ksm

import bholten.ksm.http.api.KafkaRoutes
import bholten.ksm.http.routes.KafkaStreamsRoutes
import bholten.ksm.streams.api.StartupHandler
import bholten.ksm.streams.handlers.KafkaFailFast
import bholten.ksm.streams.programs.StreamApp
import cats.effect.{ IO, IOApp }
import org.apache.kafka.streams.Topology

trait Streamlet extends IOApp.Simple {
  val topology: Topology
  val routes: KafkaRoutes[IO]     = KafkaStreamsRoutes[IO]()
  val beforeStart: StartupHandler = new KafkaFailFast()

  override def run: IO[Unit] = StreamApp.runF[IO](topology, routes, beforeStart).void
}
