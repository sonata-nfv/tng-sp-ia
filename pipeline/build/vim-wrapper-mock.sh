#!/bin/bash
set -e
cd vim-wrapper-mock/
docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-mock .
