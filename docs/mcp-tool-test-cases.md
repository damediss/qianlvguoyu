# MCP Tool 测试用例模板

本文定义认定管理第一批只读 MCP Tool 的测试模板。所有工具必须走 `platform-api` 受控 API，不直连业务数据库，不执行删除、提交、修改类动作。

## 1. 统一返回结构建议

成功：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

失败：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "TASK_NOT_FOUND",
    "message": "未找到对应认定任务",
    "detail": "taskId=T001"
  }
}
```

要求：

1. 不要直接把 Java 异常堆栈返回给 Agent。
2. 错误信息要能让 WeKnora / 模型理解。
3. 返回字段要稳定。
4. 不要返回过大的无关数据。
5. 第一阶段只做只读工具，不做删除、提交、修改。

## 2. 当前实现映射

| 业务目标工具名 | 当前 MCP Server 状态 | 当前可用 platform-api |
| --- | --- | --- |
| `get_certification_task_status` | 尚未以该名称注册；相近工具为 `certification.getTaskOverview` | `GET /api/certification/mcp/tasks/{taskId}/overview` |
| `get_candidate_collection_status` | 尚未以该名称注册；相近工具为 `certification.getCollectionProgress` | 聚合：`GET /api/certification/mcp/tasks/{taskId}/collection-progress`；单人详情：`GET /api/certification/practical-collection/detail/{taskId}/{candidateId}` |
| `get_examiner_group_status` | 尚未注册 | 相近：`GET /api/certification/preparation/tasks/{taskId}/assessor-groups` |
| `get_score_table_config_status` | 尚未注册 | 相近：`GET /api/certification/practical-scoring/tasks/{taskId}/setup` |

验收时以 MCP Inspector 和 WeKnora 能识别为准。如果目标工具名必须是 snake_case，需要在 `services/mcp-server/src/tools.ts` 中增加或替换工具注册。

## 3. 工具 1：get_certification_task_status

用途：

```text
查询认定任务当前状态。适用于用户询问某个认定任务当前处于哪个阶段、是否完成配置、下一步需要处理什么、负责人是谁等场景。该工具只读取数据，不修改任务。
```

建议入参：

```json
{
  "taskId": "T001"
}
```

当前实现注意事项：

- 当前 Java Controller 的 `{taskId}` 是数字 ID，不是 `T001` 业务编号。
- 如果用户输入 `T001`，工具应先通过任务列表查询映射到数字 ID，再调详情接口。

期望返回结构示例：

```json
{
  "success": true,
  "data": {
    "taskId": "T001",
    "taskName": "2026年电工职业技能等级认定",
    "currentStage": "PRACTICAL_CONFIG",
    "currentStageName": "实操配置",
    "ownerName": "王工",
    "scoreTableImported": false,
    "candidateCollectionCompleted": false,
    "nextAction": "导入实操评分表，并完成评分项匹配"
  },
  "error": null
}
```

测试项：

| 编号 | 场景 | 输入 | 期望 |
| --- | --- | --- | --- |
| T1-01 | 正常 taskId | 已存在任务 ID | 返回任务名称、阶段、状态、负责人或可解释为空字段 |
| T1-02 | 不存在 taskId | 不存在的 ID | 返回 `success=false` 或稳定空数据，并给出 `TASK_NOT_FOUND` |
| T1-03 | 空 taskId | `""` | 参数校验失败，返回可读错误 |
| T1-04 | 无权限访问 | 无 `certification.dashboard.view` | 返回权限错误，不泄露数据 |
| T1-05 | 缺少 tenantId | 不传 `X-Tenant-Id` | 返回租户或认证错误 |
| T1-06 | platform-api 异常 | 模拟 500 或超时 | MCP Server 不崩溃，返回可读错误 |
| T1-07 | 返回字段完整性 | 正常任务 | 字段稳定，不返回过大无关列表 |

## 4. 工具 2：get_candidate_collection_status

用途：

```text
查询认定任务下考生信息采集状态。适用于用户询问某个任务下考生是否完成头像、指纹、电子签名、签到、签退等采集情况。
```

建议入参：

```json
{
  "taskId": "T001",
  "candidateId": "C001"
}
```

当前实现注意事项：

- 当前 MCP Server 已有任务级聚合工具 `certification.getCollectionProgress`。
- 当前 platform-api 也有单个考生采集详情：`GET /api/certification/practical-collection/detail/{taskId}/{candidateId}`。
- 如果要支持 `candidateId` 可选，需要在 MCP Server 中补充逻辑：只传 `taskId` 查聚合，传 `taskId + candidateId` 查单人详情。

测试项：

| 编号 | 场景 | 输入 | 期望 |
| --- | --- | --- | --- |
| T2-01 | 按 taskId 查询全部考生采集状态 | `taskId` | 返回总人数、已完成、未完成、缺失项摘要 |
| T2-02 | 按 taskId + candidateId 查询单个考生 | `taskId + candidateId` | 返回该考生头像、指纹、签名、签到等状态 |
| T2-03 | 考生不存在 | 不存在的 `candidateId` | 返回 `CANDIDATE_NOT_FOUND` |
| T2-04 | 任务不存在 | 不存在的 `taskId` | 返回 `TASK_NOT_FOUND` |
| T2-05 | 图片 fileId 为空 | 缺头像或证件照 | 返回缺失项，不报系统错误 |
| T2-06 | 指纹未采集 | `fingerprintStatus != DONE` | 返回 `fingerprintNotDone` 类可解释字段 |
| T2-07 | 电子签名未采集 | `signatureStatus != DONE` | 返回 `signatureNotDone` 类可解释字段 |
| T2-08 | 已采集但文件引用失效 | fileId 查不到 | 返回文件引用异常，不泄露内部堆栈 |

## 5. 工具 3：get_examiner_group_status

用途：

```text
查询认定任务的考评员小组配置状态。适用于用户询问考评员是否配置完成、小组长是谁、成员是否齐全等问题。
```

建议入参：

```json
{
  "taskId": "T001"
}
```

当前实现注意事项：

- 当前未发现聚合 status 接口。
- 当前可读取小组列表：`GET /api/certification/preparation/tasks/{taskId}/assessor-groups`。
- 如需“一句话状态”，建议后续在 `platform-api` 增加只读聚合接口，或在 MCP Server 中基于小组列表和成员列表组装。

测试项：

| 编号 | 场景 | 输入 | 期望 |
| --- | --- | --- | --- |
| T3-01 | 考评员小组已配置 | 有小组和成员 | 返回组长、成员数量、配置完成 |
| T3-02 | 小组长未配置 | 小组无 leader | 返回未完成原因：小组长未配置 |
| T3-03 | 考评员人数不足 | 成员少于规则要求 | 返回未满足进入下一阶段 |
| T3-04 | 任务不存在 | 不存在的 `taskId` | 返回 `TASK_NOT_FOUND` |
| T3-05 | 返回组长、成员、配置状态 | 正常任务 | 字段完整且稳定 |
| T3-06 | 返回是否满足进入下一阶段条件 | 正常任务 | 返回 `canProceed` 和原因 |

## 6. 工具 4：get_score_table_config_status

用途：

```text
查询实操评分表和评分项配置状态。适用于用户询问评分表是否导入、评分标准是否匹配、是否允许进入平板评分等问题。
```

建议入参：

```json
{
  "taskId": "T001"
}
```

当前实现注意事项：

- 当前未发现 `score-table/status` 聚合接口。
- 当前可用相近接口：`GET /api/certification/practical-scoring/tasks/{taskId}/setup`。
- 当前 MCP Server 有实操评分进度工具 `certification.getPracticalScoreProgress`，但它更偏评分执行进度，不等同评分表配置状态。

测试项：

| 编号 | 场景 | 输入 | 期望 |
| --- | --- | --- | --- |
| T4-01 | 评分表已导入 | 已有评分表 | 返回已导入、评分项数量、可进入后续流程 |
| T4-02 | 评分表未导入 | 无评分表 | 返回未导入和下一步建议 |
| T4-03 | 评分项匹配失败 | 模板不匹配 | 返回失败项和可读原因 |
| T4-04 | 评分标准缺失 | 缺评分标准 | 返回缺失标准列表 |
| T4-05 | 不同试题评分要素数量不一致 | 多试题配置异常 | 返回异常试题和差异摘要 |
| T4-06 | 返回是否允许进入平板评分 | 正常或异常配置 | 返回 `allowTabletScoring` |
| T4-07 | 返回下一步建议 | 任意状态 | 返回明确 `nextAction` |

## 7. 通用异常测试

| 场景 | 期望 |
| --- | --- |
| token 过期 | 返回认证失败，不调用平台敏感数据 |
| tenantId 缺失 | 返回租户上下文错误 |
| 参数类型错误 | 返回参数校验错误 |
| platform-api 500 | 返回可读错误，MCP Server 保持运行 |
| platform-api 返回非 JSON | 返回 `PLATFORM_INVALID_JSON` |
| platform-api 超时 | 返回 `PLATFORM_TIMEOUT` |
| 无权限 | 返回 `PERMISSION_DENIED` |

## 8. 注册验收记录模板

| 工具名 | Inspector 可见 | inputSchema 正确 | tools/call 成功 | 异常可读 | WeKnora 可调用 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| `get_certification_task_status` | 待测 | 待测 | 待测 | 待测 | 待测 | 待定 |
| `get_candidate_collection_status` | 待测 | 待测 | 待测 | 待测 | 待测 | 待定 |
| `get_examiner_group_status` | 待测 | 待测 | 待测 | 待测 | 待测 | 待定 |
| `get_score_table_config_status` | 待测 | 待测 | 待测 | 待测 | 待测 | 待定 |
