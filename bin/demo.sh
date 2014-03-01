#!/usr/bin/env bash

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

lib=$bin/../abacus-demo/target/lib
dist=$bin/../abacus-demo/target
classes=$bin/../abacus-demo/target/test-classes
resources=$bin/../abacus-demo/src/main/resources

HEAP_OPTS="-Xmx1g -Xms1g -XX:NewSize=256m"
JAVA_OPTS="-server -d64"

MAIN_CLASS="abacus.demo.Application"
CLASSPATH=$resources/:$classes/:$lib/*:$dist/*:$1/ext/*

java $JAVA_OPTS $JMX_OPTS $HEAP_OPTS -classpath $CLASSPATH $MAIN_CLASS $bin/../car-idx
