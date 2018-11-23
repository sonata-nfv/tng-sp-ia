#!/bin/bash
set -e
set -x
cd vim-wrapper-mock
export DOCKER_HOST="unix:///var/run/docker.sock"

#Clean the workspace
#docker-compose -f docker-compose-test.yml down
docker rm -fv $(docker ps -a -f name=vimwrappermock -q) || true
docker rmi $(docker images -f reference=vimwrappermock* -q) || true
docker network rm  $(docker network ls -f name=vimwrappermock* -q) || true

#Start the container and run tests using the docker-compose-test file
docker-compose -f docker-compose-test.yml up --abort-on-container-exit
docker-compose -f docker-compose-test.yml ps
docker-compose -f docker-compose-test.yml down
