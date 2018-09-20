#!/bin/bash
set -e

TAG=$1

if [ "$TAG" == "int" ]; then
	docker tag registry.sonata-nfv.eu:5000/vim-wrapper-mock:latest registry.sonata-nfv.eu:5000/vim-wrapper-mock:int
fi

docker push registry.sonata-nfv.eu:5000/vim-wrapper-mock":$TAG"
