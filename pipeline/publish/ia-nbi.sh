#!/bin/bash
set -e

TAG=$1

if [ "$TAG" == "int" ]; then
	docker tag registry.sonata-nfv.eu:5000/ia-nbi:latest registry.sonata-nfv.eu:5000/ia-nbi:int
fi

docker push registry.sonata-nfv.eu:5000/ia-nbi":$TAG"
