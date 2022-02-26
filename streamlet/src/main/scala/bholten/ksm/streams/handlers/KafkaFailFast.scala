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

package bholten.ksm.streams.handlers

import bholten.ksm.streams.api.StartupHandler
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

class KafkaFailFast extends StartupHandler {
  override def beforeStart(kafkaStreams: KafkaStreams): KafkaStreams = {
    kafkaStreams.setUncaughtExceptionHandler(
      new StreamsUncaughtExceptionHandler {
        override def handle(
            exception: Throwable
        ): StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse =
          StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
      }
    )
    kafkaStreams
  }
}
