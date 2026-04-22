#!/bin/bash
#
# Smoke test for the distroless Web3Signer image.
#
# Runs the container with `--read-only` and no `--tmpfs` mount to prove that
# nothing anywhere on the rootfs is written at startup. If this test fails,
# something regressed that started writing outside /opt/web3signer/native-libs
# (e.g. a new dependency that triggers Netty native transport extraction) —
# investigate before masking it with `--tmpfs /tmp`.

set -euo pipefail

IMAGE="${1:?Usage: $0 <image> <reports-dir>}"
REPORTS_DIR="${2:?Usage: $0 <image> <reports-dir>}"
mkdir -p "$REPORTS_DIR"
REPORT="$REPORTS_DIR/smoke-report.txt"

NAME="w3s_distroless_smoke_$$"
cleanup() {
  docker logs "$NAME" >> "$REPORT" 2>&1 || true
  docker rm -f "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Running distroless smoke test against $IMAGE" | tee "$REPORT"

docker run -d --name "$NAME" \
  --read-only \
  --sysctl net.ipv6.conf.all.disable_ipv6=1 \
  -p 9000:9000 \
  "$IMAGE" \
  --http-listen-host=0.0.0.0 eth2 --slashing-protection-enabled=false

for i in $(seq 1 30); do
  if curl -fsS http://localhost:9000/upcheck >/dev/null 2>&1; then
    echo "upcheck passed after ${i}s" | tee -a "$REPORT"
    exit 0
  fi
  sleep 1
done

echo "upcheck never succeeded" | tee -a "$REPORT"
exit 1
