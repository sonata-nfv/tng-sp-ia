#!/bin/bash
set -e
cd ia-nbi/

#Clean the workspace
docker rm -fv $(docker ps -a -f name=ia-nbi -q) || true
docker rmi $(docker images -f reference=ia-nbi* -q) || true

docker build -t registry.sonata-nfv.eu:5000/ia-nbi .
