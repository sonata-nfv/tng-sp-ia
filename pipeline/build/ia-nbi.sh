#!/bin/bash
set -e
cd ia-nbi/

#Clean the workspace
docker rm -fv $(docker ps -a -f name=ia-nbi -q)
docker rmi $(docker images -f reference=ia-nbi* -q)

docker build -t registry.sonata-nfv.eu:5000/ia-nbi .
