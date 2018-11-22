#!/bin/bash
set -e
set -x
cd ia-nbi
export DOCKER_HOST="unix:///var/run/docker.sock"

#Clean the workspace
docker-compose -f docker-compose-test.yml down

#Start the container and run tests using the docker-compose-test file
docker-compose -f docker-compose-test.yml up --abort-on-container-exit
docker-compose -f docker-compose-test.yml ps
docker-compose -f docker-compose-test.yml down
