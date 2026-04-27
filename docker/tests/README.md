# docker/tests

Fixtures for the default-variant Docker smoke test (`docker/test.sh`).

- `goss-linux-amd64`, `goss-linux-arm64` — [goss](https://github.com/goss-org/goss) binaries used by `dgoss` to assert post-startup state inside the running container. The test script picks one based on `uname -m`.
- `dgoss` — the upstream `extras/dgoss/dgoss` helper that wraps `docker run` around `goss`.
- `01/goss.yaml`, `01/goss_wait.yaml` — the assertions for the `eth2` subcommand startup case.

## Pinned versions

All three files are from [`goss v0.4.9`](https://github.com/goss-org/goss/releases/tag/v0.4.9).

| File | SHA256 |
| ---- | ------ |
| `goss-linux-amd64` | `87dd36cfa1b8b50554e6e2ca29168272e26755b19ba5438341f7c66b36decc19` |
| `goss-linux-arm64` | `14fd24ac08236559f4809e6a627792d1b947ed98654bba1662ef1d6122d77e18` |
| `dgoss`            | `7ee35d6ccbe1440eb2a08984a43e8b3742f2e849abdc0d7384ac08de55682d7c` |

The two `goss-linux-*` checksums match the upstream `.sha256` files published alongside each release asset — regenerate them with:

```sh
curl -fsSL https://github.com/goss-org/goss/releases/download/v0.4.9/goss-linux-amd64.sha256
curl -fsSL https://github.com/goss-org/goss/releases/download/v0.4.9/goss-linux-arm64.sha256
```

## Updating

```sh
VER=v0.4.9   # change to the desired release
cd docker/tests
for arch in amd64 arm64; do
  curl -fsSL -o "goss-linux-$arch" "https://github.com/goss-org/goss/releases/download/$VER/goss-linux-$arch"
  curl -fsSL "https://github.com/goss-org/goss/releases/download/$VER/goss-linux-$arch.sha256" | shasum -a 256 -c -
  chmod +x "goss-linux-$arch"
done
curl -fsSL -o dgoss "https://raw.githubusercontent.com/goss-org/goss/$VER/extras/dgoss/dgoss"
chmod +x dgoss
shasum -a 256 goss-linux-amd64 goss-linux-arm64 dgoss   # update the table above
```
