#!/bin/bash
set -e
set -x
cd wim-wrapper-vtn
export DOCKER_HOST="unix:///var/run/docker.sock"

#Clean the workspace
docker-compose -f docker-compose-test.yml down

if ! [[ "$(docker inspect -f {{.State.Running}} wim-wrapper-vtn 2> /dev/null)" == "" ]]; then docker rm -fv wim-wrapper-vtn ; fi
docker run --name wim-wrapper-vtn -d -t registry.sonata-nfv.eu:5000/wim-wrapper-vtn
docker cp wim-wrapper-vtn:/adaptor/target/adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar .
docker rm -fv wim-wrapper-vtn
sudo chown jenkins: adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar

#Start the container and run tests using the docker-compose-test file
docker-compose -f docker-compose-test.yml up --abort-on-container-exit
docker-compose -f docker-compose-test.yml ps
docker-compose -f docker-compose-test.yml down