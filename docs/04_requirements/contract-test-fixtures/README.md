# 合同派生 POC 拟真源文档

本目录包含 3 份完整、可打开、可被 `ContractDocxDerivationService` 识别和派生的脱敏 DOCX。它们用于替代早期简单技术夹具，作为测试环境中的拟真源文档；全部主体、金额、联系人和项目均为虚构数据，不得用于真实签署或作为历史业务证据。

## 文档与覆盖结构

| 文件 | 核心结构 | 预期用途 |
| --- | --- | --- |
| `01_软件技术服务框架协议_源文档.docx` | 正文跨 run 字段、交付/付款表、页眉页脚、十个合同章节、签署区 | 验证跨 run 替换和正文样式继承 |
| `02_软件系统开发实施合同_源文档.docx` | 甲乙方/金额/项目/范围位于相邻表格单元格，里程碑表、付款表、签署区 | 验证表格标签和值单元格识别 |
| `03_信息平台运维服务合同_源文档.docx` | 正文变量、SLA/付款表、3 个 section、横向附件、不同页眉页脚、签署区 | 验证多 section、横向页面和页眉页脚替换 |

三份文档均包含 POC 所需五类标签：

```text
甲方：
乙方：
合同金额：
项目名称：
服务范围：
```

## 可重复生成

```powershell
python scripts/generate_contract_source_fixtures.py
```

生成器使用 A4 合同版式、宋体正文、黑体标题、显式页边距、固定 DXA 表格几何、页眉页脚和签署表。表格的 `tblW`、`tblInd`、`tblGrid` 与所有 `tcW` 已通过一致性审计。

## 派生回归

```powershell
mvn -f services/platform-api/pom.xml -Dtest=ContractRealisticSourceFixtureTest test
```

回归要求：

- 三份文档的 `derivationStatus` 均为 `derivable`。
- 五类字段全部识别。
- 新值替换后旧值无残留。
- 派生前后段落、表格、行列、section、页眉和页脚拓扑一致。

当前工作站未安装 LibreOffice/Word 渲染引擎，因此标准 `render_docx.py` 无法生成逐页 PNG。已完成结构、样式、section、标题、表格几何和可访问性审计，但视觉验收仍需在带渲染引擎的测试环境执行。
