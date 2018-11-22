#!/bin/bash
set -e
cd wim-wrapper-mock/
docker build -t registry.sonata-nfv.eu:5000/wim-wrapper-mock .
