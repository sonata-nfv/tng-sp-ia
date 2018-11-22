#!/bin/bash
set -e

TAG=$1

if [ "$TAG" == "int" ]; then
	docker tag registry.sonata-nfv.eu:5000/vim-wrapper-ovs:latest registry.sonata-nfv.eu:5000/vim-wrapper-ovs:int
fi

docker push registry.sonata-nfv.eu:5000/vim-wrapper-ovs":$TAG"
