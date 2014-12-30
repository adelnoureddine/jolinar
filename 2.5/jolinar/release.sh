#!/bin/bash
rm -rf jolinar.run
mvn clean install
cat stub.sh target/jolinar-*.jar > ./jolinar && chmod +x ./jolinar