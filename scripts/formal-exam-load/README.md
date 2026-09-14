# Formal-exam isolated load toolkit

This directory is the only supported way to run 10/100/500/1000/3000 Pad capacity tests. Dry-run remains the default catalog. Live writes are refused unless the process JDBC URL is the isolated schema `gjjddb_load` and both confirmation variables are set.

Do **not** point this toolkit at `43.138.163.7`, `gjjddb2026`, `gjjddbpro`, or c017.

## Layout

| Path | Role |
| --- | --- |
| `docker-compose.yml` | Isolated MySQL 8.4 (`gjjddb_load`), Redis, RabbitMQ, nginx WS proxy |
| `nginx/nginx.conf` | HTTP + WebSocket Upgrade to api-1/api-2 |
| `.env.example` | Copy to `.env` (gitignored) |
| `run.mjs` | Catalog, seed, gate, live runner, reconcile, aggregate |
| `ctl.mjs` | start / status / stop / fault injection |
| `lib/` | Gate, N-user state machine, seed, metrics, reconcile |
| `tests/` | Gate, percentile, shard, WS URL, answer versions, aggregate |

API processes run on the host (JDK 21) against containerized data stores. `SPRING_PROFILES_ACTIVE=loadtest` is mandatory. The loadtest profile has **no** production JDBC default; `FormalExamLoadtestIsolationGuard` aborts startup if the URL is shared/prod.

`ctl.mjs start` passes explicit resource caps to both host-side API JVMs so a
16 GB workstation does not reserve the JDK's roughly 4 GB default max heap for
each node. Defaults are `Xms=256m`, `Xmx=1024m`, `Xss=512k`,
`MaxMetaspaceSize=384m`, `MaxDirectMemorySize=256m`, and a 128 MB code cache.
Override them with `FORMAL_EXAM_API_*` in `.env`, or use `API_1_*` / `API_2_*`
variants for a node-specific comparison. OOM crash files are written under
`.run/hs_err_pid%p-api-*.log`.

## One-shot commands

```bat
cd scripts\formal-exam-load
copy .env.example .env
npm install
npm test

REM infra + two API nodes. Does not delete unrelated containers or volumes.
node ctl.mjs start
node ctl.mjs status

REM empty-schema Flyway on api-1; non-idempotent history is repaired only on gjjddb_load.
node run.mjs seed --pads 10 --questions 8 --consoles 1 --exam-duration-sec 240

REM live write still blocked until:
set FORMAL_EXAM_LOAD_ALLOW_LARGE=1
set FORMAL_EXAM_LOAD_ISOLATED=I_UNDERSTAND
set FORMAL_EXAM_LOAD_COMPOSE=1
node run.mjs live --scenario smoke --write-answers --base-url http://127.0.0.1:18080/api

node ctl.mjs stop
```

`ctl.mjs stop` only `docker compose -p formal-exam-load stop` plus the two API pid files. It does not `down -v`.

Empty-schema Flyway on a brand-new `gjjddb_load` hits historical ADD/DROP scripts that are not idempotent. `ctl.mjs start` boots api-1 until healthy. If a migration fails, it replays that version's remaining SQL on the isolated schema, ignores already-applied DDL (`Duplicate column`, `Can't DROP` missing index), marks the Flyway row success, and retries. It does not edit committed Flyway files and does not touch shared/prod databases. Repair evidence: `.run/flyway-repair.json`.

## Seed

`node run.mjs seed` talks only to `gjjddb_load`. It creates tenant `10159` / code `loadtest`, console users `load-console-N`, synthetic players `199xxxxxxxx`, a READY formal session, paper snapshots, and `waiting_enter` CandidateExam rows. Fixture: `.run/fixture.json` (gitignored).

Pad login uses the player mobile as account. Console login uses tenantCode `loadtest`.

## Live pad state machine

Each virtual Pad is an independent account + deviceId + token + AccessSession + WebSocket:

login → context → enter → paper → ticket → WS → 20s heartbeat → status 60–120s jitter → answer sync → submit or unified deadline → ACK → HTTP reconcile

`--pads N` creates N agents. `--shard-index` / `--shard-count` split the fixture. `aggregate` dedupes pad indexes.

Scenarios: `smoke`, `L1`–`L4`, `soak`, `offline-burst` (5 min pause + 20-item flush), `unified-deadline`, `duplicate-submit`.

## Fault injection

```bat
node ctl.mjs restart-api-1
node ctl.mjs restart-api-2
node ctl.mjs fault-redis
node ctl.mjs fault-rabbit
```

Redis down: HTTP answers must still work; ticket/WS return 503. RabbitMQ down is coexistence only, not the formal-exam realtime backbone (MySQL outbox + Redis Pub/Sub).

## Metrics

Live mode lists `/actuator/metrics` first and scrapes names that actually exist (`formal_exam.*`, `jdbc.connections.*`, Druid if present). It does not treat `hikaricp.*` as the business pool.

nginx uses round-robin (not `ip_hash`) so a single load-client IP splits identical load-test nodes evenly. It also reuses upstream HTTP/1.1 connections and resolves the Docker Desktop host gateway with IPv6 disabled; this avoids per-request host-bridge TCP churn and the synthetic unreachable AAAA route. WebSocket stays on the node that accepted the upgrade. Live reports include `nodeSplit` from each node's `http.server.requests` COUNT delta; a dual-node run is only valid when both shares are in 40%–60%.

Suggested thresholds in the capacity report are **建议值**, not a signed SLA.

## Safety

Writes and pads>10 require all of:

1. Process JDBC host/schema from `/api/actuator/info` `formalExamLoadtest`
2. Schema `gjjddb_load`
3. URL without `43.138.163.7` / `gjjddb2026` / `gjjddbpro`
4. Profile `loadtest`
5. `FORMAL_EXAM_LOAD_ALLOW_LARGE=1`
6. `FORMAL_EXAM_LOAD_ISOLATED=I_UNDERSTAND`

Checking `--base-url=http://127.0.0.1:8080` alone is not enough.
