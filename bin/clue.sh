#!/usr/bin/env bash

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

lib=$bin/../abacus-cli/target/lib
dist=$bin/../abacus-cli/target

HEAP_OPTS="-Xmx1g -Xms1g -XX:NewSize=256m"
JAVA_OPTS="-server -d64"

MAIN_CLASS="abacus.clue.commands.AbacusClient"
CLASSPATH=$CLASSPATH:$lib/*:$dist/*:$1/ext/*

(cd $bin/..; java $JAVA_OPTS $JMX_OPTS $HEAP_OPTS -classpath $CLASSPATH $MAIN_CLASS $@)
