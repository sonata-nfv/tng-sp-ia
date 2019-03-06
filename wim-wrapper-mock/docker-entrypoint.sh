#!/bin/bash

/setenv.sh

echo '{"type": "D", "timestamp": "'$(date "+%Y-%m-%d %H:%M:%S UTC")'", "component": "wim-wrapper-mock", "operation": "docker-entrypoint", "message": "Waiting for rabbitmq on port '$broker_port'"}'

while ! nc -z $broker_host $broker_port; do
  sleep 1 && echo -n .; # waiting for rabbitmq
done;

echo '{"type": "D", "timestamp": "'$(date "+%Y-%m-%d %H:%M:%S UTC")'", "component": "wim-wrapper-mock", "operation": "docker-entrypoint", "message": "Waiting for postgresql on port '$repo_port'"}'

while ! nc -z $repo_host $repo_port; do
  sleep 1 && echo -n .; # waiting for postgresql
done;

service son-sp-infra start
