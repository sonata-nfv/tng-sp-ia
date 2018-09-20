#!/bin/bash
set -e
set -x
cd vim-wrapper-heat
export DOCKER_HOST="unix:///var/run/docker.sock"

#Clean the workspace
docker-compose -f docker-compose-test.yml down

if ! [[ "$(docker inspect -f {{.State.Running}} vim-wrapper-heat 2> /dev/null)" == "" ]]; then docker rm -fv vim-wrapper-heat ; fi
docker run --name vim-wrapper-heat -d -t registry.sonata-nfv.eu:5000/vim-wrapper-heat
docker cp vim-wrapper-heat:/adaptor/target/adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar .
docker rm -fv vim-wrapper-heat
sudo chown jenkins: adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar

#Start the container and run tests using the docker-compose-test file
docker-compose -f docker-compose-test.yml up --abort-on-container-exit
docker-compose -f docker-compose-test.yml ps
docker-compose -f docker-compose-test.yml down