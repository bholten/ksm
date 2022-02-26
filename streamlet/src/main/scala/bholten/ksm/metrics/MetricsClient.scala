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

package bholten.ksm.metrics

import bholten.ksm.config.StreamletConfig.MetricsConfig
import com.timgroup.statsd.{ NoOpStatsDClient, NonBlockingStatsDClientBuilder, StatsDClient }

trait MetricsClient {
  val client: StatsDClient
}

object MetricsClient {
  final case class StatsD(metricsConfig: MetricsConfig) extends MetricsClient {
    val client: StatsDClient =
      new NonBlockingStatsDClientBuilder()
        .prefix(metricsConfig.prefix)
        .hostname(metricsConfig.hostname)
        .port(metricsConfig.port)
        .build()
  }

  final case object NoOpClient extends MetricsClient {
    override val client: StatsDClient = new NoOpStatsDClient
  }
}
