#!/bin/bash
set -e
cd wim-wrapper-mock/
docker rm -fv $(docker ps -a -f name=wim-wrapper-mock -q) || true
docker rmi $(docker images -f reference=wim-wrapper-mock* -q) || true

docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-mock .
