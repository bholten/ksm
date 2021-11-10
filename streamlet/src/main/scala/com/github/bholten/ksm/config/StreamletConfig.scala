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

import cats.effect.IO
import com.github.bholten.ksm.streams.StreamsStartupHandler
import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.{ SaslConfigs, SslConfigs }
import org.apache.kafka.common.security.auth.SecurityProtocol._
import org.apache.kafka.streams.StreamsConfig

import java.time.Duration
import java.util.Properties
import scala.util.Try

/**
  */
object StreamletConfig {
  def loadDefault: IO[StreamletConfig] = IO(ConfigFactory.load()).map(cfg => StreamletConfig(cfg))

  def loadDefaultUnsafe: StreamletConfig = StreamletConfig(ConfigFactory.load())

  final case class HttpConfig(config: Config) {
    val host: String = config.getString("streamlet.http.server.host")
    val port: Int    = config.getInt("streamlet.http.server.port")
  }

  final case class MetricsConfig(config: Config) {
    val prefix: String   = config.getString("streamlet.metrics.prefix")
    val hostname: String = config.getString("streamlet.metrics.hostname")
    val port: Int        = config.getInt("streamlet.metrics.port")
  }
}

final case class StreamletConfig(config: Config) {
  import Implicits._
  import StreamletConfig._

  val httpConfig: HttpConfig       = HttpConfig(config)
  val metricsConfig: MetricsConfig = MetricsConfig(config)

  val applicationId: String = config.getString("streamlet.kafka.streams.application.id")
  val startupTimeoutDuration: Duration =
    config.getDuration("streamlet.kafka.startup.timeout.duration")

  val startUpHandler: StreamsStartupHandler = {
    val className = config.getString("streamlet.startup.handler")
    this.getClass.getClassLoader
      .loadClass(className)
      .newInstance()
      .asInstanceOf[StreamsStartupHandler]
  }

  val kafkaProperties: Properties = {
    val props = new Properties

    props.put(
      StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
      config.getString("streamlet.kafka.consumer.auto.offset.reset")
    )
    props.put(
      StreamsConfig.consumerPrefix(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG),
      config.getLong("streamlet.kafka.consumer.heartbeat.interval.ms").toString
    )
    props.put(
      StreamsConfig.consumerPrefix(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG),
      config.getInt("streamlet.kafka.consumer.session.timeout.ms").toString
    )
    props.put(
      StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG),
      config.getString("streamlet.kafka.producer.acks")
    )
    props.put(
      StreamsConfig.APPLICATION_ID_CONFIG,
      applicationId
    )
    props.put(
      StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
      config.getString("streamlet.kafka.streams.bootstrap.servers")
    )
    props.put(
      StreamsConfig.REPLICATION_FACTOR_CONFIG,
      config.getInt("streamlet.kafka.streams.replication.factor")
    )
    props.put(StreamsConfig.STATE_DIR_CONFIG, config.getString("streamlet.kafka.streams.state.dir"))
//    props.put(
//      StreamsConfig.ACCEPTABLE_RECOVERY_LAG_CONFIG,
//      config.getLong("streamlet.kafka.streams.acceptable.recovery.lag").toString
//    )
    props.put(
      StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG,
      config.getLong("streamlet.kafka.streams.cache.max.bytes.buffering").toString
    )
    props.put(StreamsConfig.CLIENT_ID_CONFIG, config.getString("streamlet.kafka.streams.client.id"))
    props.put(
      StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
      config.getString("streamlet.kafka.streams.default.deserialization.exception.handler")
    )
    props.put(
      StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
      config.getString("streamlet.kafka.streams.default.key.serde")
    )
    props.put(
      StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG,
      config.getString("streamlet.kafka.streams.default.production.exception.handler")
    )
    props.put(
      StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
      config.getString("streamlet.kafka.streams.default.timestamp.extractor")
    )
    props.put(
      StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
      config.getString("streamlet.kafka.streams.default.value.serde")
    )
    config
      .getStringOption("streamlet.kafka.streams.default.windowed.key.serde.inner")
      .foreach { keySerdeInner =>
        props.put(
          StreamsConfig.DEFAULT_WINDOWED_KEY_SERDE_INNER_CLASS,
          keySerdeInner
        )
      }
    config
      .getStringOption("streamlet.kafka.streams.default.windowed.value.serde.inner")
      .foreach { valueSerdeInner =>
        props.put(
          StreamsConfig.DEFAULT_WINDOWED_VALUE_SERDE_INNER_CLASS,
          valueSerdeInner
        )
      }
    props.put(
      StreamsConfig.MAX_TASK_IDLE_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.max.task.idle.ms").toString
    )
//    props.put(
//      StreamsConfig.MAX_WARMUP_REPLICAS_CONFIG,
//      config.getInt("streamlet.kafka.streams.max.warmup.replicas").toString
//    )
    props.put(
      StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG,
      config.getInt("streamlet.kafka.streams.num.standby.replicas").toString
    )
    props.put(
      StreamsConfig.NUM_STREAM_THREADS_CONFIG,
      config.getInt("streamlet.kafka.streams.num.stream.threads").toString
    )
    props.put(
      StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
      config.getString("streamlet.kafka.streams.processing.guarantee")
    )

    // Security
    val security = Try {
      config.getString("streamlet.kafka.streams.security.protocol")
    }.map {
      case "PLAINTEXT"      => PLAINTEXT
      case "SASL_PLAINTEXT" => SASL_PLAINTEXT
      case "SASL_SSL"       => SASL_SSL
      case "SSL"            => SSL
      case s                => new IllegalStateException(s"Security protocol not supported $s")
    }.getOrElse(PLAINTEXT)

    props.put(
      StreamsConfig.SECURITY_PROTOCOL_CONFIG,
      security.toString
    )

    // Protocol-specific config
    security match {
      case PLAINTEXT => ()
      case SASL_PLAINTEXT =>
        props.put(
          SaslConfigs.SASL_JAAS_CONFIG,
          config.getString("streamlet.kafka.security.sasl.jaas.config")
        )
        props.put(
          SaslConfigs.SASL_MECHANISM,
          config.getString("streamlet.kafka.security.sasl.mechanism")
        )
      case SASL_SSL =>
        // SSL Configs are technically not required
        // Confluent Cloud, for example, has public CA-signed certs, so the
        // key stores are not required.
        for {
          keyPassword <- config.getStringOption("streamlet.kafka.security.ssl.key.password")
          ksLocation  <- config.getStringOption("streamlet.kafka.security.ssl.keystore.location")
          ksPassword  <- config.getStringOption("streamlet.kafka.security.ssl.keystore.password")
          tsLocation  <- config.getStringOption("streamlet.kafka.security.ssl.truststore.location")
          tsPassword  <- config.getStringOption("streamlet.kafka.security.ssl.truststore.password")
        } yield {
          props.put(
            SslConfigs.SSL_KEY_PASSWORD_CONFIG,
            keyPassword
          )
          props.put(
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
            ksLocation
          )
          props.put(
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
            ksPassword
          )
          props.put(
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            tsLocation
          )
          props.put(
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
            tsPassword
          )
        }

        // SASL Required for this config
        props.put(
          SaslConfigs.SASL_JAAS_CONFIG,
          config.getString("streamlet.kafka.security.sasl.jaas.config")
        )
        props.put(
          SaslConfigs.SASL_MECHANISM,
          config.getString("streamlet.kafka.security.sasl.mechanism")
        )
      case SSL =>
        props.put(
          SslConfigs.SSL_KEY_PASSWORD_CONFIG,
          config.getString("streamlet.kafka.security.ssl.key.password")
        )
        props.put(
          SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
          config.getString("streamlet.kafka.security.ssl.keystore.location")
        )
        props.put(
          SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
          config.getString("streamlet.kafka.security.ssl.keystore.password")
        )
        props.put(
          SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          config.getString("streamlet.kafka.security.ssl.truststore.location")
        )
        props.put(
          SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
          config.getString("streamlet.kafka.security.ssl.truststore.password")
        )
    }

//    props.put(
//      StreamsConfig.TASK_TIMEOUT_MS_CONFIG,
//      config.getLong("streamlet.kafka.streams.task.timeout.ms").toString
//    )
    props.put(
      StreamsConfig.TOPOLOGY_OPTIMIZATION,
      config.getString("streamlet.kafka.streams.topology.optimization")
    )
    props.put(
      StreamsConfig.APPLICATION_SERVER_CONFIG,
      config.getString("streamlet.kafka.streams.application.server")
    )
    props.put(
      StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG,
      config.getInt("streamlet.kafka.streams.buffered.records.per.partition").toString
    )
    props.put(
      StreamsConfig.BUILT_IN_METRICS_VERSION_CONFIG,
      config.getString("streamlet.kafka.streams.built.in.metrics.version")
    )
    props.put(
      StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.commit.interval.ms").toString
    )
    props.put(
      StreamsConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.connections.max.idle.ms").toString
    )
    props.put(
      StreamsConfig.METADATA_MAX_AGE_CONFIG,
      config.getLong("streamlet.kafka.streams.metadata.max.age.ms").toString
    )
    props.put(
      StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG,
      config.getString("streamlet.kafka.streams.metric.reporters")
    )
    props.put(
      StreamsConfig.METRICS_NUM_SAMPLES_CONFIG,
      config.getInt("streamlet.kafka.streams.metrics.num.samples").toString
    )
    props.put(
      StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG,
      config.getString("streamlet.kafka.streams.metrics.recording.level")
    )
    props.put(
      StreamsConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.metrics.sample.window.ms").toString
    )
    props.put(
      StreamsConfig.POLL_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.poll.ms").toString
    )
//    props.put(
//      StreamsConfig.PROBING_REBALANCE_INTERVAL_MS_CONFIG,
//      config.getLong("streamlet.kafka.streams.probing.rebalance.interval.ms").toString
//    )
    props.put(
      StreamsConfig.RECEIVE_BUFFER_CONFIG,
      config.getInt("streamlet.kafka.streams.receive.buffer.bytes").toString
    )
    props.put(
      StreamsConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.reconnect.backoff.max.ms").toString
    )
    props.put(
      StreamsConfig.RECONNECT_BACKOFF_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.reconnect.backoff.ms").toString
    )
    props.put(
      StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG,
      config.getInt("streamlet.kafka.streams.request.timeout.ms").toString
    )
    props.put(
      StreamsConfig.RETRY_BACKOFF_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.retry.backoff.ms").toString
    )
    config
      .getStringOption("streamlet.kafka.streams.rocksdb.config.setter")
      .foreach { rocksDbConfig =>
        props.put(
          StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG,
          rocksDbConfig
        )
      }
    props.put(
      StreamsConfig.SEND_BUFFER_CONFIG,
      config.getInt("streamlet.kafka.streams.send.buffer.bytes").toString
    )
    props.put(
      StreamsConfig.STATE_CLEANUP_DELAY_MS_CONFIG,
      config.getLong("streamlet.kafka.streams.state.cleanup.delay.ms").toString
    )
    config
      .getStringOption("streamlet.kafka.streams.upgrade.from")
      .foreach { upgradeFromConfig =>
        props.put(
          StreamsConfig.UPGRADE_FROM_CONFIG,
          upgradeFromConfig
        )
      }
    props.put(
      StreamsConfig.WINDOW_STORE_CHANGE_LOG_ADDITIONAL_RETENTION_MS_CONFIG,
      config
        .getLong("streamlet.kafka.streams.windowstore.changelog.additional.retention.ms")
        .toString
    )
    props
  }
}
