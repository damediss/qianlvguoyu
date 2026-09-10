# Formal-exam load (Phase 6)

Runnable planner for 100/500/1000/3000 pads and 1/5/10 consoles. **Dry-run is the default.** Live 3000-pad / 3000-socket numbers are not claimed from this machine.

Do **not** point a large live run at `43.138.163.7` (shared SaaS MySQL). Isolated DB + Redis + one or more `platform-api` nodes only.

## Commands

```bat
node scripts/formal-exam-load/run.mjs
node scripts/formal-exam-load/run.mjs --scenario 3000 --consoles 10
node scripts/formal-exam-load/run.mjs --live --mode health --pads 8 --duration-sec 10 --base-url http://127.0.0.1:8080/api
node scripts/formal-exam-load/run.mjs --live --mode metrics --base-url http://127.0.0.1:8080/api
```

Read-only exam protocol smoke (login / context / status / ticket / WS). No answer writes:

```bat
node scripts/formal-exam-load/run.mjs --live --mode exam --session-id <snowflake> --account <player> --password <pwd> --pads 1 --duration-sec 8
```

`--write-answers` is refused against `43.138.163.7`.

## Data prep for a real 3000-pad run (not executed here)

1. Isolated MySQL 8.4 with the same Flyway chain as SaaS, including `V20260909_005`–`007`.
2. Redis for tickets / presence / Pub/Sub.
3. Seed one `RUNNING` formal exam session with N candidate exams and paper snapshots.
4. N player accounts that can `POST /competition/formal-exam/player/auth/login` with that `sessionId`.
5. 1–10 admin users with `competition:exam-session:view` (plus force-submit/offline if those cases run).
6. Start 1 then 2 `platform-api` processes with distinct `COMPETITION_FORMAL_EXAM_NODE_ID`.
7. Capture `GET /api/actuator/metrics/*` plus MySQL `SHOW ENGINE INNODB STATUS` / Redis `INFO` during the run.
8. After reconnect burst, reconcile IndexedDB-equivalent client payloads vs `competition_formal_candidate_answer`.

## Scenario map

| # | Scenario | Script | This environment |
| --- | --- | --- | --- |
| 1 | 100/500/1000/3000 pads × 1/5/10 consoles | dry-run catalog | planned only |
| 2 | 3000 WS steady | catalog + exam WS smoke | WS smoke at 1 connection if API/Redis up |
| 3 | Answer every 10s | catalog | not live-written |
| 4 | 5 min offline, 20-item burst | catalog | not live-written |
| 5 | Unified deadline | catalog + unit tests | unit only |
| 6 | Rolling restart / Redis down / cross-node | catalog + unit tests | Redis-down 503 unit; multi-instance not deployed |
| 7 | Duplicate login / generation / late ACK | unit tests | unit |
| 8 | Force-submit / 清退 | unit tests + UI E2E | unit; UI if servers up |

## Metrics (no PII)

- `GET /api/actuator/health`
- `GET /api/actuator/metrics/formal_exam.http`
- `GET /api/actuator/metrics/hikaricp.connections.active`
- Redis: ticket/presence key counts, not token values
