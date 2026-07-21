# 实操信息采集表 PDF 打印版交付说明

## 1. 交付范围

本次采用 Excel + PDF 双输出：

- Excel：保留原有 Apache POI 导出，用于查看和二次编辑。
- PDF：新增 Apache PDFBox 固定模板输出，作为正式打印版，不经过 Excel 转 PDF。

## 2. 接口

`POST /api/certification/collection/export/pdf`

请求体：

```json
{
  "examTaskId": "认定任务 ID",
  "candidateIds": ["已完成实操采集的考生 ID"]
}
```

权限：`certification.practical.collection.export`。

返回：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "fileId": "文件 ID",
    "fileName": "实操采集打印版_任务名_时间.pdf",
    "downloadUrl": "/api/files/{fileId}/download"
  }
}
```

PDF 生成后通过现有 `FileApplicationService` 上传，文件内容不写入业务表。

## 3. 模板说明

- 页面：A4 横向，固定页边距，固定 3 列 × 3 行，即 9 名考生/页。
- 页眉：任务名称、考试时间、考场信息。
- 每个考生区域高度固定，不因姓名、证件号或图片尺寸变化。
- 每个考生格按参考模板排列：证件照、指纹、签名列；签名列固定包含“考场签到”和“交卷确认”两个槽位，底部包含姓名/实操、证件号、准考证三行。
- 图片框固定：证件照、指纹、签到签名、交卷签名。当前数据库没有独立的交卷指纹字段，按参考模板不额外增加版面槽位。
- 图片按比例缩放后居中绘制；无法读取或为空时显示“暂无采集图片”。
- 文本超出固定宽度时截断，不自动换行；不会产生标题单独占页或考生跨页。
- 中文字体优先读取 `COLLECTION_PDF_FONT_PATH`，其次探测常见系统字体路径，并嵌入 PDF；支持 TrueType `.ttf` 和 TrueType Collection `.ttc`。

当前数据库只有最近一次指纹记录和有效签名记录，没有独立的签到/交卷两套文件字段。为保持不改库并复用现有采集数据，当前记录会填充签到和交卷两个对应槽位；后续若数据模型增加阶段字段，只需调整导出数据组装，不需要改模板布局。

## 4. 前端入口

实操采集管理台勾选已完成考生后显示：

- `生成 Excel`
- `生成打印版 PDF`

PDF 返回文件信息后，前端通过带鉴权的文件服务下载。

## 5. 验证结果

模板测试使用 10 名考生、不同图片尺寸，并让第 10 名考生的签名图片为空：

- 生成 2 页 PDF，分页为 9 + 1。
- PDF 页面尺寸为 A4（约 595.28 × 841.89 pt）。
- 中文标题、考生姓名和“暂无采集图片”可被 PDF 文本提取器读取。
- 已渲染第一页和最后一页 PNG，确认固定框线、图片比例、空图占位和分页位置稳定。

验证产物：`outputs/practical-collection-qa/generated-collection-form-test.pdf`、`outputs/practical-collection-qa/rendered-pdf/page-1.png`、`page-4.png`。

## 6. 已知事项

- 真实数据联调仍需在已配置文件存储和至少 10 名已完成考生数据的环境中调用接口验证；当前自动化测试覆盖模板渲染，不伪造数据库/文件服务联调结果。
- Linux/容器部署必须安装 `fonts-wqy-zenhei` 并使用 `/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc`，或设置 `COLLECTION_PDF_FONT_PATH` 指向可读的 TrueType `.ttf/.ttc` 文件；CFF outlines 的 `.otf` 不受 PDFBox `PDType0Font` 支持。
