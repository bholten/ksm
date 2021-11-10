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

package com.github.bholten.ksm.config

import com.typesafe.config.Config

private object Implicits {
  implicit class ConfigOps(val config: Config) {
    def getStringOption(path: String): Option[String] = getOptional(path, config.getString)

    private def getOptional[T](path: String, get: String => T): Option[T] =
      if (config.hasPath(path))
        Option(get(path))
      else
        None

    def getIntOption(path: String): Option[Int] = getOptional(path, config.getInt)
  }
}
