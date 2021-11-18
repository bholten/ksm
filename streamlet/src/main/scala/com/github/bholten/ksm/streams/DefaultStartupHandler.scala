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

package com.github.bholten.ksm.streams

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

class DefaultStartupHandler extends StreamsStartupHandler with LazyLogging {
  override def beforeStart(streams: KafkaStreams, cb: Either[Throwable, Unit] => Unit): Unit = {
    streams.setUncaughtExceptionHandler(new StreamsUncaughtExceptionHandler {
      override def handle(
          exception: Throwable
      ): StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse = {
        logger.error(s"Kafka Streams caught on exception ${exception.getMessage}")
        logger.debug(
          s"Kafka Streams stacktrace: ${exception.getStackTrace.mkString("Array(", ", ", ")")}"
        )
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
      }
    })

    streams.start()
  }
}
