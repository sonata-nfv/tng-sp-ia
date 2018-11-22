#!/bin/bash
set -e
cd vim-wrapper-ovs/
docker build -t registry.sonata-nfv.eu:5000/vim-wrapper-ovs .
