#!/bin/bash
set -e
cd wim-wrapper-vtn/
docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-vtn .
