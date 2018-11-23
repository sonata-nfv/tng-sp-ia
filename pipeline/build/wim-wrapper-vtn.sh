#!/bin/bash
set -e
cd wim-wrapper-vtn/
docker rm -fv $(docker ps -a -f name=wim-wrapper-vtn -q) || true
docker rmi $(docker images -f reference=wim-wrapper-vtn* -q) || true

docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-vtn .
