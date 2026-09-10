# 竞赛理论正式考试 API / WebSocket 合同

与当前 `platform-api` + `apps/admin-web` 实现对齐。雪花 ID 一律 JSON 字符串。

## 权限

| 能力 | 权限码 |
| --- | --- |
| 控制台查看 / 签发 console ticket | `competition:exam-session:view` |
| 准备 / 开考 | `competition:exam-session:prepare` / `start` |
| 强制交卷 | `competition:exam-session:force-submit` |
| 清退 | `competition:exam-session:force-offline` |
| 选手考试 | 考试 Sa-Token（AccessSession + generation） |

## 选手 HTTP

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/competition/formal-exam/player/auth/login` | `account,password,sessionId,deviceId` → token + accessSessionId + generation |
| GET | `/api/competition/formal-exam/player/sessions/{id}/context` | 首屏快照 |
| GET | `/api/competition/formal-exam/player/sessions/{id}/status` | 轻量状态 |
| POST | `.../enter` | 进入在考 |
| GET | `.../paper` | 试卷快照 |
| POST | `.../answers/sync` | 批量答案；stale/idempotent/conflict/new-version 逐题 ACK |
| POST | `.../submit` | 提交前版本屏障 |
| POST | `/api/competition/formal-exam/player/realtime/ticket` | Redis 一次性 ticket；Redis 宕机 503 |
| POST | `/api/competition/formal-exam/player/realtime/ack` | HTTP ACK，generation 栅栏 |

401 清除选手 token / current player（deviceId 可保留）。

## 管理端 HTTP

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/competition/formal-exam-sessions/{id}` | 场次快照（人数 GROUP BY，非全场聚合扫描） |
| GET | `/api/competition/formal-exam-sessions/{id}/candidates` | **真分页** |
| POST | `.../start` | 开考 |
| POST | `.../candidates/{candidateExamId}/force-submit` | 强交，HTTP 先行 |
| POST | `.../candidates/{candidateExamId}/force-offline` | 清退，HTTP 先行，不改答卷 |
| POST | `.../realtime/ticket` | console ticket，与 player ticket 前缀隔离 |

## WebSocket

- Player：`GET /api/ws/competition/formal-exam/player?ticket=`
- Console：`GET /api/ws/competition/formal-exam/console?ticket=`
- Handshake 不要求 `X-Tenant-Id`；租户从 ticket 写入 `TenantContextHolder`。

Envelope（无答案正文、证件号、手机号、原始 token）：

```json
{
  "eventId": "string",
  "commandId": "string|null",
  "type": "session.status.changed|candidate.status.changed|candidate.progress.changed|access-session.kicked|candidate.force-submit.completed|snapshot.reload.required|command.ack|application.heartbeat|candidate.presence.changed",
  "examSessionId": "string",
  "candidateExamId": "string|null",
  "accessSessionId": "string|null",
  "generation": 1,
  "seq": 1,
  "target": "examSession|accessSession|console",
  "payload": {}
}
```

- `TARGET_ACCESS` 不跨 Pad；同场 console 可收。
- Presence/progress 可按 `console.subscribe-page` 过滤；status/kick/session/force-submit 不过滤。
- Presence `seq = null`。
- 旧 generation / 非 active AccessSession：关闭 socket（stale generation）。
- ACK：`{ "type":"command.ack","commandId","generation" }`，CAS `issued`→`acked`；重复 ACK 幂等。

## 幂等与 fencing

- 答案：低 clientVersion = stale；同版本同内容幂等；同版本不同内容 conflict。
- 提交 / 自动交 / 强交 / 缺考：行锁 + CAS，禁止双终态与终态回退。
- 同设备再登录：generation+1 并踢旧 device token。
- 他设备在 active 会话期间登录：409。
- 清退后可再登录，generation 递增。
