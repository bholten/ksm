#!/bin/sh

kafka-topics localhost:9092 --create --topic input-topic --replication-factor 1 --partitions 1
kafka-topics localhost:9092 --create --topic output-topic --replication-factor 1 --partitions 1
