#!/bin/bash

if [ -z "${DOCKER_TAG}" ]; then
    echo "Missing DOCKER_TAG environment variable"
    exit 1
fi

project=oystr-cloud-test
name=oystr-vault-service

echo "Tagging image with ${DOCKER_TAG}" &&
  docker tag "${name}":"${DOCKER_TAG}" us.gcr.io/"${project}"/"${name}":"${DOCKER_TAG}"

echo "Pushing image to remote repo" &&
  $(command -v gcloud) docker -- push us.gcr.io/"${project}"/"${name}":"${DOCKER_TAG}"

echo "Done"
