#!/bin/bash
set -euo pipefail

ENV=${1:-dev}
ENV_FILE="docker/env/.env.${ENV}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: $ENV_FILE not found"
  echo "Usage: $0 {local|dev|qa|prod}"
  exit 1
fi

echo "========================================="
echo "  Deploying ACS to: $ENV"
echo "========================================="

# Export env vars for docker stack (which doesn't support --env-file)
set -a
source "$ENV_FILE"
set +a

docker stack deploy \
  -c docker/swarm-stack.yml \
  acs

echo ""
echo "Deployed. Useful commands:"
echo "  docker stack services acs"
echo "  docker service logs -f acs_gateway-service"
echo "  docker service scale acs_gateway-service=3"
echo "  docker service update --image acs/gateway-service:v2 acs_gateway-service"
