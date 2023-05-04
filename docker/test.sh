#!/bin/bash

set -e

export GOSS_PATH=tests/goss-linux-amd64
export GOSS_OPTS="$GOSS_OPTS --format junit"
export GOSS_FILES_STRATEGY=cp

DOCKER_IMAGE=$1
DOCKER_TEST_IMAGE=web3signer_goss

# create test docker image that includes the test key file and password files
TEST_CONTAINER_ID=$(docker create ${DOCKER_IMAGE})
docker commit ${TEST_CONTAINER_ID} ${DOCKER_TEST_IMAGE}

i=0

# Test for normal startup with ports opened
GOSS_FILES_PATH=tests/01 \
bash tests/dgoss \
run ${DOCKER_TEST_IMAGE} \
--http-listen-host=0.0.0.0 \
eth2 \
--slashing-protection-enabled=false \
> ./reports/01.xml || i=`expr $i + 1`

docker image rm ${DOCKER_TEST_IMAGE}

# also check for security vulns with trivy
docker run aquasec/trivy image $DOCKER_IMAGE

exit $i
