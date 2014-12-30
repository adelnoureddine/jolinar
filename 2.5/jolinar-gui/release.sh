#!/bin/bash
rm -rf jolinar-gui
mvn clean package -Pstandalone
cat stub.sh target/jolinar-*-jar-with-dependencies.jar > ./jolinar-gui
chmod a+x ./jolinar-gui