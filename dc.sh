#!/bin/bash

function up {
  docker volume prune -f
  docker-compose -p asset-onboarding -f service/docker/dependencies.yaml up --build -d
  docker ps -a
}

function down {
  docker-compose -p asset-onboarding -f service/docker/dependencies.yaml down
}

function bounce {
   down
   up
}

function local_specs {

  ./cli/bin/cli write-specs-asset \
      --contract-spec-id "18573cf8-ddb9-491e-a4cb-bf2176160a63" \
      --scope-spec-id "997e8228-c37f-4668-9a66-6cfb3b2a23cd" \
      --key-mnemonic "jealous bright oyster fluid guide talent crystal minor modify broken stove spoon pen thank action smart enemy chunk ladder soon focus recall elite pulp" \
      --chain-id local-chain \
      --node https://127.0.0.1:9090

  ./cli/bin/cli write-specs-loan-state \
      --contract-spec-id "63a8bb4c-c6e0-4cb5-993b-b134c4b5cbbb" \
      --scope-spec-id "2eeada14-07cb-45fe-af6d-fdc48b627817" \
      --key-mnemonic "jealous bright oyster fluid guide talent crystal minor modify broken stove spoon pen thank action smart enemy chunk ladder soon focus recall elite pulp" \
      --chain-id local-chain \
      --node https://127.0.0.1:9090
}

${1}
