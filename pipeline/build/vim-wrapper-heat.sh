#!/bin/bash
set -e
cd vim-wrapper-heat/
#Clean the workspace
docker rm -fv $(docker ps -a -f name=vim-wrapper-heat -q) || true
docker rmi $(docker images -f reference=vim-wrapper-heat* -q) || true

docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-heat .
