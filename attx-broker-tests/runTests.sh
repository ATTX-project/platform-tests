#!/bin/sh

if [ $BASE = "pdTest" ]; then
    echo "Testing the whole platform need to check for health status"
    dockerize -wait tcp://mysql:3306 -timeout 240s
    dockerize -wait http://fuseki:3030 -timeout 60s
    dockerize -wait http://graphmanager:4302/health -timeout 60s
    dockerize -wait http://uvprov:4301/health -timeout 60s
    dockerize -wait http://provservice:7030/health -timeout 60s
    # dockerize -wait http://essiren:9200 -timeout 60s
    dockerize -wait http://es5:9210 -timeout 60s
else
    echo "Testing specific parts of the platform. Do not wait for anything. Health checks done in tests."
fi

echo  "Archiva repository URL: $REPO"


gradle -b /tmp/build.gradle -PartifactRepoURL=$REPO containerIntegTest