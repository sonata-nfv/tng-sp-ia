#!/bin/bash
set -e
docker build -t registry.sonata-nfv.eu:5000/ia-nbi . ia-nbi/Dockerfile .
docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-heat vim-wrapper-heat/Dockerfile .
docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-mock vim-wrapper-mock/Dockerfile .
docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-ovs vim-wrapper-ovs/Dockerfile .
docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-mock wim-wrapper-mock/Dockerfile .
docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-vtn wim-wrapper-vtn/Dockerfile .