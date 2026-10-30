# Isolated S2 local Hermes (work in progress)

This uses the real, unmodified Hermes gateway, not the S1 mock. Server source is
pinned to Android's gateway contract SHA
`4d04303818b61bf3aa09278af48356315515965b`. `hermes serve` runs the same REST/WS
backend as dashboard without building its browser frontend. Only core + `web`
Python dependencies are selected from the upstream frozen lock.

## Prepare

From this directory, with Docker Desktop running:

```sh
# Use a checkout fetched from https://github.com/NousResearch/hermes-agent
# at the exact SHA above, never a moving branch:
sh prepare-source.sh /tmp/hermes-s2-server-source
python3 configure.py
docker compose config --quiet
docker compose build
docker compose up -d
```

`configure.py` reads the OpenAI API key with hidden terminal input, creates a
separate random dashboard password and signing secret, and never prints them.
Existing secrets are preserved. The local directory is mode 0700; secret files
are read-only 0444 so Compose bind-mounted secrets work with container UID 10000.
Other host users cannot traverse the private directory. `.local/` is ignored; its contents must never
be added to git. Build context excludes secrets. Model: `gpt-4.1-mini`, provider
`openai-api`. No production credentials or host home directories are used.

Server exposes **only `127.0.0.1:19120`**. Add the separate S2 connection through
the existing Hermes login UI, username `s2`, password from the local secret
file (do not paste into chat, command arguments or reports). Keep the existing
19119 mock profile. For the approved emulator only:

```sh
/Users/sergei/Library/Android/sdk/platform-tools/adb -s emulator-5554 reverse tcp:19120 tcp:19120
```

The host endpoint in Android is `http://127.0.0.1:19120/`. No application release
policy changes are required. API login, image delivery, model response and
history remain NOT_RUN until actual evidence is recorded in `docs/s2/REPORT.md`.

## Isolation and lifecycle

Project name `hermes-s2-local`; dedicated named data/workspace volumes; nonroot
UID 10000; read-only root filesystem; writable private volumes and bounded
`/tmp`; all capabilities dropped; no-new-privileges; 2 CPUs, 4 GiB RAM, 256 PIDs;
Docker logs limited to three 10 MiB files. Outbound network is required for
OpenAI. No Telegram configuration, OpenViking, host home/SSH mounts, Docker
socket, privileged mode or production service configuration. This is a test
container, not a hostile-code sandbox: use only the harmless S2 prompts.

`docker compose stop` stops only this project. `docker compose down` preserves
its data volumes. Do not use `down -v` unless deliberate deletion of S2 history
is wanted. Never operate on the unrelated production projects on this Mac.
