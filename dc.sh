#!/bin/bash

function up {
  docker volume prune -f
  docker-compose -f service/docker/dependencies.yaml up --build -d
  docker ps -a
}

function down {
  docker-compose -f service/docker/dependencies.yaml down
}

function bounce {
   down
   up
}

${1}
