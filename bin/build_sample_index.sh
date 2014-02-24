#!/usr/bin/env bash

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

lib=$bin/../abacus-core/target/lib
dist=$bin/../abacus-core/target
classes=$bin/../abacus-core/target/test-classes

HEAP_OPTS="-Xmx1g -Xms1g -XX:NewSize=256m"
JAVA_OPTS="-server -d64"

MAIN_CLASS="abacus.search.facets.IndexGenerator"
CLASSPATH=$classes/:$lib/*:$dist/*:$1/ext/*

java $JAVA_OPTS $JMX_OPTS $HEAP_OPTS -classpath $CLASSPATH $MAIN_CLASS $bin/../abacus-core/src/test/resources/cars.json $@