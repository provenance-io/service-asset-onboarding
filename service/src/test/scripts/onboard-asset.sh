#!/bin/bash

SERVICE_ASSET_ONBOARDING_HOST=localhost:8080

# jealous bright oyster fluid guide talent crystal minor modify broken stove spoon pen thank action smart enemy chunk ladder soon focus recall elite pulp
PUBLIC_KEY=AyxS63kwfSSLbPsqSvVi5APUgmuw9UDwJLvDk3Uo9usL
ADDRESS=tp1mryqzguyelef5dae7k6l22tnls93cvrc60tjdc

scope_id=$1
asset_file=$2

curl --http1.1 --silent --location --request POST http://${SERVICE_ASSET_ONBOARDING_HOST}/api/v1/asset/${scope_id} \
  --header "Content-Type: application/json" \
  --header "Accept: application/json" \
  --header "x-public-key: ${PUBLIC_KEY}" \
  --header "x-address: ${ADDRESS}" \
  --data "@${asset_file}"
