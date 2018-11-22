#!/bin/bash
set -e
cd vim-wrapper-heat/
docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-heat .
