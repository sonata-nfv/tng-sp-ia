#!/bin/bash
set -e
set -x
cd ia-nbi
export DOCKER_HOST="unix:///var/run/docker.sock"

#Clean the workspace
docker-compose -f docker-compose-test.yml down

if ! [[ "$(docker inspect -f {{.State.Running}} ia-nbi 2> /dev/null)" == "" ]]; then docker rm -fv ia-nbi ; fi
docker run --name ia-nbi -d -t registry.sonata-nfv.eu:5000/ia-nbi
docker cp ia-nbi:/adaptor/target/adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar .
docker rm -fv ia-nbi
sudo chown jenkins: adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar

#Start the container and run tests using the docker-compose-test file
docker-compose -f docker-compose-test.yml up --abort-on-container-exit
docker-compose -f docker-compose-test.yml ps
docker-compose -f docker-compose-test.yml down