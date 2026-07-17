# MCP 配置踩坑记录（2026-07-09）

> **场景**：WeKnora Agent 调用认定管理 MCP（`rending_mcp` / `certification-data-assistant`）  
> **环境**：`43.138.163.7` — WeKnora（Docker）+ MCP Server（宿主机 pm2）+ platform-api（宿主机 JAR）  
> **关联文档**：[`mcp-local-test-flow.md`](./mcp-local-test-flow.md)、[`mcp-tool-test-cases.md`](./mcp-tool-test-cases.md)

---

## 1. 整体链路

认定管理 MCP 是三层调用，**每一层都有独立配置**，任何一层不通都会导致「看起来配好了但实际用不了」：

```text
WeKnora Agent（智能推理）
    ↓ Bearer Token
MCP Server（:3001，/opt/guoyu-platform/mcp-server）
    ↓ PLATFORM_API_TOKEN + X-Tenant-Id
platform-api（:8080，/api/certification/mcp/*）
    ↓
MySQL
```

已注册工具（4 个，只读）：

| MCP Tool | 平台 API |
| --- | --- |
| `certification.searchTasks` | `GET /api/certification/mcp/tasks` |
| `certification.getTaskOverview` | `GET /api/certification/mcp/tasks/{taskId}/overview` |
| `certification.getCollectionProgress` | `GET /api/certification/mcp/tasks/{taskId}/collection-progress` |
| `certification.getPracticalScoreProgress` | `GET /api/certification/mcp/tasks/{taskId}/practical-score-progress` |

权限要求：`certification.dashboard.view`

---

## 2. 配置层：「MCP 已配好」≠「Agent 会调工具」

### 坑 1：Agent 模式是「快速问答」

| 项 | 说明 |
| --- | --- |
| **现象** | MCP 服务测试通过，对话却只检索知识库，不调工具 |
| **根因** | `quick-answer` 只做 RAG，**不会**发起 MCP 工具调用 |
| **处理** | Agent 模式改为 **智能推理**（`smart-reasoning` / ReAct） |

### 坑 2：Agent 绑了 MCP，Embed 渠道绑的是另一个 Agent

| 项 | 说明 |
| --- | --- |
| **现象** | 后台编辑的 Agent 已绑定 `rending_mcp`，云平台「认定小助手」弹窗仍只查知识库 |
| **根因** | Embed 渠道独立绑定 Agent，不会自动跟随你刚编辑的那个 |
| **处理** | 在 WeKnora 后台确认 Embed 渠道（如 `aa3a676b-a1cb-4719-8e06-6636932d9bb2`）绑定的也是带 MCP 的 Agent |

代码中的默认渠道 ID（`apps/admin-web/src/components/ai/WeKnoraWidget.tsx`）：

```text
DEFAULT_CHANNEL_ID = 'aa3a676b-a1cb-4719-8e06-6636932d9bb2'
```

### 坑 3：MCP 选择模式未保存为 `selected`

| 项 | 说明 |
| --- | --- |
| **现象** | Agent 回复「当前对话环境中未提供实时数据查询工具」 |
| **根因** | `mcp_selection_mode` 为空或 `none`，运行时未注入 MCP tools |
| **处理** | 设为 `selected`，勾选 `rending_mcp`，保存后 **新建对话** 再测 |

### 坑 4：系统提示词与 MCP 冲突

| 项 | 说明 |
| --- | --- |
| **现象** | 问「当前进行中的认定任务」，Agent 答「知识库未提供依据，无法判断」 |
| **根因** | 提示词强调「只能依据知识库」「必须优先检索知识库」，模型不会去调 MCP |
| **处理** | 改为 **双通道**：制度/流程/表单 → 知识库；任务状态/进度/考生数 → MCP 工具 |

### 坑 5：Cursor MCP 与 WeKnora MCP 互不相通

| 项 | 说明 |
| --- | --- |
| **现象** | WeKnora 里 MCP 已通，Cursor Agent 说未挂载 certification MCP |
| **根因** | 两套独立的 MCP 配置 |
| **处理** | 各自单独配置；WeKnora Agent 联调以 WeKnora 后台为准 |

---

## 3. 网络层：同机部署 ≠ 同网可达

WeKnora 跑在 Docker 容器（`WeKnora-app`），MCP 跑在宿主机（pm2，端口 3001）。**浏览器能通、宿主机 curl 能通，不代表 WeKnora 容器内能通。**

### 坑 6：容器内访问公网 IP 回环失败

| 项 | 说明 |
| --- | --- |
| **现象** | WeKnora MCP 测试报「网络错误」；宿主机 `curl http://43.138.163.7:3001/health` 返回 200 |
| **根因** | Docker 容器内访问本机公网 IP（hairpin NAT）经常超时 |
| **验证** | `docker exec WeKnora-app curl -s http://43.138.163.7:3001/health` → FAIL |
| **处理** | 不要用公网 IP 作为 MCP URL |

### 坑 7：Docker 网关 IP 被 SSRF 拦截

| 项 | 说明 |
| --- | --- |
| **现象** | URL 改为 `http://172.17.0.1:3001/mcp` 后保存失败 |
| **根因** | WeKnora 默认 **禁止 MCP URL 使用直连 IP** |
| **报错** | `SSRF validation failed: direct IP address` |
| **处理** | 不能单独依赖 `172.17.0.1`（需配合白名单，见坑 8） |

### 坑 8：`host.docker.internal` 默认也在 SSRF 限制名单

| 项 | 说明 |
| --- | --- |
| **现象** | `http://host.docker.internal:3001/mcp` 保存报 400 |
| **根因** | `host.docker.internal` 属于受限主机名，需显式加白 |
| **报错** | `SSRF validation failed: hostname host.docker.internal is restricted` |
| **处理** | 在 WeKnora `.env` 增加并重启： |

```bash
# /opt/guoyu-platform/WeKnora/.env
SSRF_WHITELIST_EXTRA=searxng,qdrant,milvus,weaviate,doris-fe,43.138.163.7,host.docker.internal,172.17.0.0/16

cd /opt/guoyu-platform/WeKnora
docker compose restart app
```

### 坑 9：MCP URL 端口写错

| 项 | 说明 |
| --- | --- |
| **现象** | 配置列表里出现过 `http://43.138.163.7:8888/mcp` |
| **根因** | 端口应为 **3001**（WeKnora 自身可能是 8088） |
| **处理** | 统一为 `http://host.docker.internal:3001/mcp` |

### 最终可用的 WeKnora MCP 配置

| 字段 | 值 |
| --- | --- |
| 传输方式 | HTTP Streamable（失败可试 SSE：`/sse`） |
| URL | `http://host.docker.internal:3001/mcp` |
| Bearer Token | 与 `/opt/guoyu-platform/mcp-server/.env` 中 `MCP_BEARER_TOKEN` **字符级一致** |

---

## 4. 鉴权与上游层：MCP 连通 ≠ 能查到数据

### 坑 10：MCP 测试成功，工具调用返回 HTTP 404

| 项 | 说明 |
| --- | --- |
| **现象** | `certification.searchTasks` 调用失败，返回 `HTTP 404: Resource not found` |
| **根因** | 服务器 `platform-api` 是旧 JAR（7 月 3 日部署），**不含** `CertificationMcpController` |
| **验证** | `jar tf platform-api-0.1.0-SNAPSHOT.jar \| grep CertificationMcp` → 无结果 |
| **处理** | 构建含 MCP 接口的新 JAR 并部署，备份旧 JAR 后重启 |

```bash
# 宿主机验证接口是否存在
curl -s -o /dev/null -w '%{http_code}' \
  'http://127.0.0.1:8080/api/certification/mcp/tasks?page=1&pageSize=1'
# 旧 JAR：404；新 JAR：200 或 401（需 Token）
```

### 坑 11：`PLATFORM_API_TOKEN` 过期

| 项 | 说明 |
| --- | --- |
| **现象** | MCP → platform-api 返回 400/401 |
| **根因** | MCP Server `.env` 中的平台 Token 失效 |
| **处理** | 重新登录获取 Token，更新 `.env` 后 `pm2 restart` |

```bash
# 1. 登录拿新 token
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantCode":"千铝工投","username":"admin","password":"<密码>"}'

# 2. 更新 /opt/guoyu-platform/mcp-server/.env
#    PLATFORM_API_TOKEN=<新 token>

# 3. 重启 MCP
cd /opt/guoyu-platform/mcp-server
pm2 restart ecosystem.config.cjs
```

### 坑 12：`.env` 含 Windows 换行符（CRLF）

| 项 | 说明 |
| --- | --- |
| **现象** | URL/Token 看起来正确，但请求异常 |
| **根因** | 在 Windows 上编辑 `.env` 带入 `\r`，导致值尾部带隐藏字符 |
| **处理** | 统一转为 LF：`sed -i 's/\r$//' .env` 或用 Linux 编辑器修改 |

### 坑 13：platform-api 重启时 `java: command not found`

| 项 | 说明 |
| --- | --- |
| **现象** | 替换 JAR 后服务起不来 |
| **根因** | 服务器 `java` 不在默认 PATH |
| **处理** | 使用完整路径启动 |

```bash
cd /opt/guoyu-platform
nohup /usr/local/jdk-21.0.11+10/bin/java \
  -jar platform-api-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod >> nohup.out 2>&1 &
```

### MCP Server `.env` 参考

```text
MCP_BEARER_TOKEN=<WeKnora 访问 MCP 的 Bearer，两边必须一致>
PLATFORM_API_BASE_URL=http://localhost:8080/api
PLATFORM_API_TOKEN=<平台登录 token，需 certification.dashboard.view>
PLATFORM_TENANT_ID=1
```

> MCP 在宿主机运行，`localhost:8080` 是正确的；不要用公网 IP 访问 platform-api。

---

## 5. 使用层细节

### 坑 14：工具默认「需人工审核」

| 项 | 说明 |
| --- | --- |
| **现象** | Agent 发起工具调用后卡住，等用户操作 |
| **根因** | 4 个工具均标记「需人工审核」 |
| **处理** | 测试阶段点「批准」；稳定后可关闭审核策略 |

### 坑 15：用浏览器可达代替容器内可达

| 项 | 说明 |
| --- | --- |
| **现象** | 本机浏览器能打开 MCP health，WeKnora 仍连不上 |
| **根因** | 浏览器、宿主机、Docker 容器是三条不同网络路径 |
| **处理** | 必须在 **WeKnora 容器内**验证：`docker exec WeKnora-app curl http://host.docker.internal:3001/health` |

---

## 6. 推荐排查顺序

以后 MCP 出问题，按此顺序查，避免反复踩坑：

```text
1. WeKnora 后台 → MCP 服务 →「测试连接」
   → 应看到 4 个 certification.* 工具

2. Agent 配置
   → 模式 = 智能推理（smart-reasoning）
   → mcp_selection_mode = selected，勾选 rending_mcp
   → Embed 渠道绑同一 Agent
   → 提示词区分「知识库」vs「实时数据」

3. 网络（在 WeKnora 容器内）
   → curl http://host.docker.internal:3001/health

4. 上游（在宿主机）
   → curl http://127.0.0.1:8080/api/certification/mcp/tasks（带 Token + X-Tenant-Id）

5. 部署版本
   → jar tf platform-api*.jar | grep CertificationMcpController

6. Token 与进程
   → 更新 PLATFORM_API_TOKEN → pm2 restart mcp-server
```

---

## 7. 2026-07-09 最终跑通配置

| 环节 | 状态 | 关键配置 |
| --- | --- | --- |
| WeKnora MCP 连接 | ✅ | `http://host.docker.internal:3001/mcp` + SSRF 白名单 |
| Agent 工具调用 | ✅ | `smart-reasoning` + 双通道提示词 |
| platform-api MCP 接口 | ✅ | 新 JAR 含 `CertificationMcpController` |
| 端到端查询 | ✅ | `searchTasks` 返回 15 条认定任务实时数据 |

验证问题示例：

```text
查一下当前进行中的认定任务
```

成功标志：出现工具调用轨迹，返回具体任务编号、考生数、阶段等字段，并标注「数据来源：平台 MCP 实时查询」。

---

## 8. 运维 Checklist（部署 / 发版）

- [ ] 新 `platform-api` JAR 包含 `CertificationMcpController`（`jar tf` 检查）
- [ ] MCP `.env` 使用 LF 换行，Token 未过期
- [ ] WeKnora `SSRF_WHITELIST_EXTRA` 含 `host.docker.internal`
- [ ] platform-api 启动脚本使用 Java 全路径
- [ ] `pm2 status` 确认 mcp-server 在线
- [ ] 容器内 + 宿主机各 curl 一次 health / mcp tasks
- [ ] Agent Embed 渠道与编辑的 Agent 一致

---

## 9. 相关路径速查

| 组件 | 路径 / 地址 |
| --- | --- |
| MCP Server 代码 | `services/mcp-server/` |
| MCP 生产部署 | `/opt/guoyu-platform/mcp-server/` |
| platform-api 生产 JAR | `/opt/guoyu-platform/platform-api-0.1.0-SNAPSHOT.jar` |
| WeKnora 配置 | `/opt/guoyu-platform/WeKnora/.env` |
| MCP 健康检查 | `http://<host>:3001/health` |
| MCP 端点 | `http://<host>:3001/mcp`（Streamable）或 `/sse` |
| 认定小助手 Embed 渠道 | `aa3a676b-a1cb-4719-8e06-6636932d9bb2` |

---

## 10. 安全提醒

- MCP Bearer Token、平台 API Token、服务器凭据**不要提交 Git**
- 若在聊天或工单中暴露过密码/Token，应**尽快轮换**
- MCP 工具均为只读查询，写操作仍需走平台审批流程
