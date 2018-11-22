#!/bin/bash
set -e
cd ia-nbi/
docker build -t registry.sonata-nfv.eu:5000/ia-nbi .
