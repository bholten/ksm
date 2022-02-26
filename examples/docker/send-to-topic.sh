#!/bin/sh

# Note: running this will give you an interactive shell.
# Whatever you type will be sent to the topic word-count-input,
# with keys and values separated by comma.

kafka-console-producer \
    --broker-list localhost:9092 \
    --topic word-count-input \
    --property parse.key=true \
    --property key.separator=,