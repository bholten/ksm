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
import kafka.log.{ Defaults, LogConfig }

import java.util
import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * Default configuraiton for Kafka Streams changelogs.
  * Not managed by Cats Effect runtime.
  */
object ChangelogConfig {
  lazy val default: util.Map[String, String] = ChangelogConfig(null).properties
}

final case class ChangelogConfig(config: Config) {
  val properties: java.util.Map[String, String] = {
    val props = new util.HashMap[String, String]()
    props.put(
      LogConfig.CleanupPolicyProp,
      Try(config.getString(LogConfig.CleanupPolicyProp)).getOrElse(Defaults.CleanupPolicy)
    )
    props.put(
      LogConfig.CompressionTypeProp,
      Try(config.getString(LogConfig.CompressionTypeProp)).getOrElse(Defaults.CompressionType)
    )
    props.put(
      LogConfig.DeleteRetentionMsProp,
      Try(config.getLong(LogConfig.DeleteRetentionMsProp))
        .getOrElse(Defaults.DeleteRetentionMs)
        .toString
    )
    props.put(
      LogConfig.FileDeleteDelayMsProp,
      Try(config.getInt(LogConfig.FileDeleteDelayMsProp))
        .getOrElse(Defaults.FileDeleteDelayMs)
        .toString
    )
    props.put(
      LogConfig.FlushMessagesProp,
      Try(config.getLong(LogConfig.FlushMessagesProp)).getOrElse(Defaults.FlushInterval).toString
    )
    props.put(
      LogConfig.FlushMsProp,
      Try(config.getLong(LogConfig.FlushMsProp)).getOrElse(Defaults.FlushMs).toString
    )
    props.put(
      LogConfig.FollowerReplicationThrottledReplicasProp,
      Try(config.getStringList(LogConfig.FollowerReplicationThrottledReplicasProp))
        .getOrElse(Defaults.FollowerReplicationThrottledReplicas)
        .asScala
        .mkString(",")
    )
    props.put(
      LogConfig.IndexIntervalBytesProp,
      Try(config.getInt(LogConfig.IndexIntervalBytesProp))
        .getOrElse(Defaults.IndexInterval)
        .toString
    )
    props.put(
      LogConfig.LeaderReplicationThrottledReplicasProp,
      Try(config.getStringList(LogConfig.LeaderReplicationThrottledReplicasProp))
        .getOrElse(Defaults.LeaderReplicationThrottledReplicas)
        .asScala
        .mkString(",")
    )
    props.put(
      LogConfig.MaxCompactionLagMsProp,
      Try(config.getLong(LogConfig.MaxCompactionLagMsProp))
        .getOrElse(Defaults.MaxCompactionLagMs)
        .toString
    )
    props.put(
      LogConfig.MaxMessageBytesProp,
      Try(config.getInt(LogConfig.MaxMessageBytesProp))
        .getOrElse(Defaults.MaxMessageSize)
        .toString
    )
    props.put(
      LogConfig.MessageDownConversionEnableProp,
      Try(config.getBoolean(LogConfig.MessageDownConversionEnableProp))
        .getOrElse(Defaults.MessageDownConversionEnable)
        .toString
    )
    props.put(
      LogConfig.MessageFormatVersionProp,
      Try(config.getString(LogConfig.MessageFormatVersionProp))
        .getOrElse(Defaults.MessageFormatVersion)
    )
    props.put(
      LogConfig.MessageTimestampDifferenceMaxMsProp,
      Try(config.getLong(LogConfig.MessageTimestampDifferenceMaxMsProp))
        .getOrElse(Defaults.MessageTimestampDifferenceMaxMs)
        .toString
    )
    props.put(
      LogConfig.MinCleanableDirtyRatioProp,
      Try(config.getDouble(LogConfig.MinCleanableDirtyRatioProp))
        .getOrElse(Defaults.MinCleanableDirtyRatio)
        .toString
    )
    props.put(
      LogConfig.MinCompactionLagMsProp,
      Try(config.getLong(LogConfig.MinCompactionLagMsProp))
        .getOrElse(Defaults.MinCompactionLagMs)
        .toString
    )
    props.put(
      LogConfig.MinInSyncReplicasProp,
      Try(config.getInt(LogConfig.MinInSyncReplicasProp))
        .getOrElse(Defaults.MinInSyncReplicas)
        .toString
    )
    props.put(
      LogConfig.PreAllocateEnableProp,
      Try(config.getBoolean(LogConfig.PreAllocateEnableProp))
        .getOrElse(Defaults.PreAllocateEnable)
        .toString
    )
    props.put(
      LogConfig.RetentionBytesProp,
      Try(config.getLong(LogConfig.RetentionBytesProp))
        .getOrElse(Defaults.RetentionSize)
        .toString
    )
    props.put(
      LogConfig.RetentionMsProp,
      Try(config.getLong(LogConfig.RetentionMsProp)).getOrElse(Defaults.RetentionMs).toString
    )
    props.put(
      LogConfig.SegmentBytesProp,
      Try(config.getInt(LogConfig.SegmentBytesProp)).getOrElse(Defaults.SegmentSize).toString
    )
    // TODO(brennan) verify this:
    /*
    props.put(
      LogConfig.SegmentIndexBytesProp,
      Try(config.getLong(LogConfig.SegmentIndexBytesProp)).getOrElse(kafka.server.Defaults.LogIndexSizeMaxBytes)
    )
     */
    props.put(
      LogConfig.SegmentJitterMsProp,
      Try(config.getLong(LogConfig.SegmentJitterMsProp))
        .getOrElse(Defaults.SegmentJitterMs)
        .toString
    )
    props.put(
      LogConfig.SegmentMsProp,
      Try(config.getLong(LogConfig.SegmentMsProp)).getOrElse(Defaults.SegmentMs).toString
    )
    props.put(
      LogConfig.UncleanLeaderElectionEnableProp,
      Try(config.getBoolean(LogConfig.UncleanLeaderElectionEnableProp))
        .getOrElse(Defaults.UncleanLeaderElectionEnable)
        .toString
    )
    props
  }
}
