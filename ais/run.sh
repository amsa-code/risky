#!/bin/bash
# Display usage
[ $# -eq 0 ] && { echo "Usage: run.sh <HOST> <PORT> [extract]"; exit 1; }
#
mvn -q exec:java -Dhost=$1 -Dport=$2 -Dtype=$3