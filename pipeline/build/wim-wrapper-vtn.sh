#!/bin/bash
set -e
cd wim-wrapper-vtn/
docker rm -fv $(docker ps -a -f name=wim-wrapper-vtn -q)
docker rmi $(docker images -f reference=wim-wrapper-vtn* -q)

docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-vtn .
