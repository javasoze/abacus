thrift  --gen java -o src/main src/main/thrift/query.thrift
cp -R src/main/gen-java/* src/main/java
rm -rf src/main/gen-java
