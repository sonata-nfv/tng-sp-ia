#!/bin/bash
set -e
cd vim-wrapper-mock/
docker rm -fv $(docker ps -a -f name=vim-wrapper-mock -q) || true
docker rmi $(docker images -f reference=vim-wrapper-mock* -q) || true

docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-mock .
