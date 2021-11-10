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

package com.github.bholten.ksm.metrics

import com.github.bholten.ksm.config.StreamletConfig
import org.slf4j.LoggerFactory

import scala.util.Try

trait StreamletMetrics {
  private val log = LoggerFactory.getLogger("StreamletMetrics")

  lazy val metrics: MetricsClient =
    Try {
      MetricsClient.StatsD(StreamletConfig.loadDefaultUnsafe.metricsConfig)
    }.getOrElse {
      log.warn("StatsD metrics client could not be created, providing no-op client")
      MetricsClient.NoOpClient
    }
}
