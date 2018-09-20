#!/bin/bash
set -e
TAG=$1

if [ "$TAG" == "int" ]; then
	docker tag registry.sonata-nfv.eu:5000/ia-nbi:latest registry.sonata-nfv.eu:5000/ia-nbi:int
	docker tag registry.sonata-nfv.eu:5000/vim-wrapper-heat:latest registry.sonata-nfv.eu:5000/vim-wrapper-heat:int
	docker tag registry.sonata-nfv.eu:5000/vim-wrapper-mock:latest registry.sonata-nfv.eu:5000/vim-wrapper-mock:int
	docker tag registry.sonata-nfv.eu:5000/vim-wrapper-ovs:latest registry.sonata-nfv.eu:5000/vim-wrapper-ovs:int
	docker tag registry.sonata-nfv.eu:5000/wim-wrapper-mock:latest registry.sonata-nfv.eu:5000/wim-wrapper-mock:int
	docker tag registry.sonata-nfv.eu:5000/wim-wrapper-vtn:latest registry.sonata-nfv.eu:5000/wim-wrapper-vtn:int
fi

docker push registry.sonata-nfv.eu:5000/ia-nbi":$TAG"
docker push registry.sonata-nfv.eu:5000/vim-wrapper-heat":$TAG"
docker push registry.sonata-nfv.eu:5000/vim-wrapper-mock":$TAG"
docker push registry.sonata-nfv.eu:5000/vim-wrapper-ovs":$TAG"
docker push registry.sonata-nfv.eu:5000/wim-wrapper-mock":$TAG"
docker push registry.sonata-nfv.eu:5000/wim-wrapper-vtn":$TAG"