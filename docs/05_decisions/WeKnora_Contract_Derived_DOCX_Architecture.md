# WeKnora 历史 DOCX 派生式合同生成架构

状态：Accepted  
日期：2026-08-14  
确认：需求方于 2026-08-14 同意全部 Review Gate  
请求 Schema：`guoyu.contract-derivation-request/v1`

## 1. 决策结论

合同生成改为“RAG 检索选源 + 原始 DOCX 快照 + 结构化修改 + 确定性渲染”。

`platform-api` 仍是合同生成的权限、业务状态、版本和审计责任方，但不再从空白 `XWPFDocument` 凭空构造合同。它必须复制已选定的历史 DOCX，在保留源结构和版式的前提下执行受控修改。

当前 `ContractPreviewGenerationService` 是文件交付链路的 E2E 联调桩，不作为生产合同渲染器的设计基础。

## 2. 为什么不能只使用 RAG 文本

RAG 通常保留提取后的文本块、摘要、元数据和向量。它能用于判断“哪份合同更相似”，但不能可靠恢复：

- 字体、字号、加粗、缩进、行距和段前段后。
- 多级编号、样式继承、分节、分页和页面设置。
- 表格宽度、合并单元格、页眉页脚、文本框和签章区。

因此知识库必须保留两类相互关联的资产：

1. 语义索引：用于 RAG 检索、对比、问答和字段提取。
2. 不可变原始 DOCX：用于生成新合同时继承文档结构与版式。

## 3. 组件责任

| 组件 | 责任 | 禁止事项 |
| --- | --- | --- |
| WeKnora 知识库 | 保留原 DOCX，生成文本索引，返回 `knowledgeId`、文件哈希和业务元数据 | 不用提取文本替代原 DOCX |
| WeKnora Agent | 检索候选、解释差异、收集用户字段、产生结构化修改指令 | 不输出 OOXML，不自由生成最终 Word 排版 |
| MCP Server | 暴露受控选源、校验、预览和定稿工具 | 不直连数据库/COS，不接受任意文件路径或接收人 |
| platform-api | 校验租户与权限，固化源文档快照，生成结构清单，派生 DOCX，校验残留，管理版本和审计 | 不信任模型提交的 URL、路径、哈希或锚点 |
| 平台文件模块 | 保存源快照和派生文件，计算 SHA-256，受控读取 | 不将对象存储路径暴露给模型 |

## 4. 源文档生命周期

```text
RAG 命中 knowledgeId
  -> contract.resolveSourceDocument
  -> platform-api 通过服务间身份读取 WeKnora 元数据和原 DOCX
  -> 校验 tenant / KB 读权限 / MIME / 大小 / SHA-256 / 作废状态
  -> 写入平台私有不可变 source snapshot
  -> 扫描结构锚点并生成 manifest
  -> 人工或受控规则将其标记为 derivable
  -> 返回 sourceSnapshotId + sourceFileHash + manifestId/version + allowedAnchors
```

生成 Worker 只读取平台已固化的不可变 source snapshot，不在异步任务中再次依赖 RAG 搜索结果或临时下载 URL。这保证同一请求可重放、可审计，且不因原知识文档后续更新而漂移。

## 5. 结构清单

每个 source snapshot 都对应一份带版本的结构清单，至少记录：

- 文档级样式、section、页边距和页眉页脚引用。
- 标题层级、多级编号、段落顺序和表格位置。
- content control、bookmark、标题和表格单元格生成的稳定 `anchorId`。
- 每个锚点允许的操作、最大内容长度、是否必须人工确认。
- 源合同中待检查的旧公司名、金额、日期、项目名和其他变量候选。

Agent 只能使用 `contract.resolveSourceDocument` 返回的 `anchorId`，不能提交 XPath、OOXML 节点、任意正则或文件内部路径。

## 6. MCP 接口

### 6.1 `contract.findSimilarContracts`

由 Agent 结合 WeKnora RAG 结果调用，用于执行合同类型、租户、部门、作废状态、保密级别和 `derivable` 过滤。

输出是候选元数据，不是文件字节。

### 6.2 `contract.resolveSourceDocument`

输入只接受 WeKnora `knowledgeBaseId` 和 `knowledgeId`。平台使用当前 BotActorContext 和服务账号验证权限，生成不可变 source snapshot 和 manifest。

输出至少包含：

```json
{
  "sourceSnapshotId": "2084569567062556674",
  "knowledgeBaseId": "ee3740b4-cd03-4fb2-84d4-667d05cfc7ac",
  "knowledgeId": "6a521abb-3182-4ea1-b911-65cb95cac43a",
  "originalFilename": "信息化技术服务框架协议.docx",
  "sourceFileHash": "<64 位小写十六进制>",
  "manifestId": "2084569567062556675",
  "manifestVersion": 1,
  "derivationStatus": "derivable",
  "allowedAnchors": []
}
```

### 6.3 `contract.generatePreview`

输入必须通过 [contract-derivation-request-v1.schema.json](../04_requirements/schemas/contract-derivation-request-v1.schema.json) 校验。

生成接口必须对 `sourceSnapshotId`、`sourceFileHash`、`manifestId/version` 执行服务端交叉校验，不得信任模型传入的文件名或选择原因。

成功后仍返回既有 `guoyu.generated-artifacts/v1`，不破坏已验收的企微文件交付链路。

## 7. DOCX 派生规则

1. 打开 source snapshot 后在新文档版本上修改，永不覆盖源文档。
2. 优先使用 content control 和 bookmark；其次使用已登记的标题/表格锚点。
3. 文本替换必须处理 Word 跨 `run` 拆分，并覆盖正文、表格、页眉、页脚和内容控件。
4. 默认保留首个被替换位置的 run properties 与所在段落样式；章节级替换按 manifest 的样式策略处理。
5. 不允许 Agent 自由修改页面设置、宏、外部关系、嵌入对象或签章图片。
6. 生成后必须执行空占位符、旧字段残留、必备章节、锚点计数和 DOCX 可重新打开校验。
7. 任何 error 阻止文件交付；warning/risk 写入草稿且要求用户确认。

Apache POI 可作为 MVP 的 DOCX 处理库，但必须通过真实历史 DOCX 回归集验证表格、页眉页脚、多级编号和内容控件。如果 POI 无法稳定保留目标文档的 OOXML 结构，再对 docx4j 做小范围技术验证，不在未验证前更换主库。

## 8. 数据与审计

每个合同版本至少保存：

- `knowledgeBaseId / knowledgeId`。
- `sourceSnapshotId / sourceFileId / sourceFileHash`。
- `manifestId / manifestVersion`。
- 结构化字段及其 `source/sourceDocumentId/confirmed`。
- 章节编辑、条款变更、生成前后哈希和残留扫描结果。
- BotActorContext、requestId、生成人、确认人、生成时间和渲染器版本。

## 9. 安全与失败策略

- 拒绝跨租户、无权知识库、作废文档、非 DOCX、哈希不一致、manifest 版本不一致和非 `derivable` 源文档。
- 不接受文件路径、URL、Base64、OOXML、XPath、任意正则或接收人作为生成参数。
- source snapshot 与派生文档都使用私有存储，只通过平台文件服务受控读取。
- 生成成功但 PDF 转换失败时保留 DOCX；生成成功但企微发送失败时保留合同版本并单独重试交付。

## 10. MVP 验收

1. 至少准备 3 份结构不同的真实脱敏历史 DOCX 作为回归集。
2. RAG 可根据用户需求返回相似合同候选，选定后生成不可变 source snapshot。
3. 替换甲方、乙方、金额、项目名和服务范围后，原文档样式、表格、页眉页脚、编号和分页保持。
4. 原文档中的旧主体、旧金额、旧日期不能在新 DOCX 中静默残留。
5. 平台能追溯新文档使用的原知识文档、原文件哈希、manifest 版本、字段来源和全部修改。
6. 产物继续通过 `guoyu.generated-artifacts/v1` 进入现有企微交付链路。

## 11. 本决策不包含

- 不在本阶段开放任意 Word 文档的无审核自动派生。
- 不让模型直接修改 OOXML、宏、嵌入对象或签章图片。
- 不改变 `guoyu.generated-artifacts/v1`、BotActorContext 和当前私聊文件交付安全边界。
