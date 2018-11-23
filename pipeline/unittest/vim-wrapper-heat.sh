#!/bin/bash
set -e
set -x
cd vim-wrapper-heat
export DOCKER_HOST="unix:///var/run/docker.sock"

#Clean the workspace
#docker-compose -f docker-compose-test.yml down
docker rm -fv $(docker ps -a -f name=vimwrapperheat -q) || true
docker rmi $(docker images -f reference=vimwrapperheat* -q) || true
docker network rm  $(docker network ls -f name=vimwrapperheat* -q) || true

#Start the container and run tests using the docker-compose-test file
docker-compose -f docker-compose-test.yml up --abort-on-container-exit
docker-compose -f docker-compose-test.yml ps
docker-compose -f docker-compose-test.yml down
