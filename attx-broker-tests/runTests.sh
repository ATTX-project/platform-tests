#!/bin/sh

# Wait for MySQL, the big number is because CI is slow.
dockerize -wait tcp://mysql:3306 -timeout 240s
dockerize -wait http://fuseki:3030 -timeout 60s
dockerize -wait http://graphmanager:4302/health -timeout 60s
dockerize -wait http://uvprov:4301/health -timeout 60s
dockerize -wait http://provservice:7030/health -timeout 60s
dockerize -wait http://essiren:9200 -timeout 60s
dockerize -wait http://es5:9210 -timeout 60s

echo  "Archiva repository URL: $REPO"

gradle -PartifactRepoURL=$REPO containerIntegTest
