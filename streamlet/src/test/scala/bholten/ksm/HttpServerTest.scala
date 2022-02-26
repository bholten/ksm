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

import bholten.ksm.http.routes.KafkaStreamsRoutes
import bholten.ksm.streams.api.StreamHandler
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request }
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class HttpServerTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  "Streams Context before Kafka Streams is started" - {
    val kafkaHandler: StreamHandler[IO] = new StreamHandler[IO] {
      override def start(): IO[Unit] = IO.unit

      override def stop(): IO[Unit] = IO.unit

      override def isHealthy: IO[Boolean] = IO(true)

      override def metrics(): IO[LazyList[(String, Double)]] = IO {
        LazyList.fill(10)(("test", 1.0))
      }

      override def describe(): IO[String] = IO("Mock")
    }
    val routes = KafkaStreamsRoutes[IO].routes(kafkaHandler)

    "Should respond with OK 200 on root" in {
      val request: Request[IO] =
        Request(method = Method.GET, uri = uri"/")
      val response = routes(request).value

      response
        .asserting {
          case Some(value) =>
            value.status.code shouldBe 200
          case None =>
            false shouldBe true
        }
    }

    "Should respond with OK 200 on health check" in {
      val request: Request[IO] =
        Request(method = Method.GET, uri = uri"/health")
      val response = routes(request).value

      response
        .asserting {
          case Some(value) =>
            value.status.code shouldBe 200
          case None =>
            false shouldBe true
        }
    }

    "Should respond with OK 200 on metrics" in {
      val request: Request[IO] =
        Request(method = Method.GET, uri = uri"/metrics")
      val response = routes(request).value

      response
        .asserting {
          case Some(value) =>
            value.status.code shouldBe 200
          case None =>
            false shouldBe true
        }
    }
  }
}
