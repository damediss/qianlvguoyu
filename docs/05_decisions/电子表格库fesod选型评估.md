# 电子表格库 fesod 选型评估：平台电子表格读写是否重构

> 日期：2026-08-30
> 评估对象：[Apache fesod (Incubating)](https://github.com/apache/fesod/) 2.0.2-incubating（2026-05 发布）
> 现状基线：`platform-api` 原生 Apache POI **5.5.1**，38 个文件电子表格读写、7 个文件 Word 处理
> 结论先行：**技术上完全可行且零依赖冲突；但不建议全量重构，建议"新代码用 fesod、存量按需迁移"的渐进策略**

---

## 1. fesod 是什么

Apache fesod（Fast · Easy · Spreadsheet & other Documents）是阿里 **EasyExcel 捐赠给 Apache 软件基金会后的孵化项目**，API 与 EasyExcel 一脉相承：

- **流式读取（核心卖点）**：基于 SAX 逐行回调（`ReadListener`），不把整个 Workbook 加载进内存，大文件不 OOM
- **注解模型**：`@ExcelProperty` 标注字段 → 一行代码完成"Excel 行 ↔ Java 对象"映射，替代手写 `getCell/getStringCellValue`
- **写入**：注解模型逐行写、样式注解（`@HeadStyle`/`@ContentStyle`）、模板填充（fill）、CSV 读写（内置 commons-csv）
- **底层仍是 Apache POI**（官方 README 明示），POI 能做的复杂操作（合并单元格、数据校验、公式）fesod 里照常可用

当前版本 **2.0.2-incubating**，构件：`fesod-sheet`（核心）、`fesod-common`、`fesod-bom`、`fesod-shaded`（fat jar，**已有 POI 的项目不要用**，会类冲突）、examples。

## 2. 项目现状盘点（实测）

| 维度 | 现状 |
|---|---|
| 依赖 | 原生 POI **5.5.1**（`poi-ooxml`，pom:169-174，为对齐 Tika 3.3.0 传递版本，已处理过 POI 双版本坑） |
| 使用面 | **38 个文件**电子表格读写（`XSSFWorkbook/HSSFWorkbook/WorkbookFactory/SXSSF`）；**7 个文件** Word（`XWPFDocument`，fesod 不覆盖） |
| 使用模式 | 全部**手写 POI**，从未用过 EasyExcel/FastExcel |
| POI import 分布 | `ss.usermodel` 211 处、`xssf.usermodel` 32 处、`xwpf.usermodel` 50 处（Word）、`xssf.streaming` 3 处（SXSSF 大导出） |

三类典型场景（决定重构收益的关键）：

| 场景 | 代表 | 特征 | fesod 收益 |
|---|---|---|---|
| A. 重样式导出 | `MubiaoExcelExporter`（36 处 CellStyle/Font/合并/数据校验）、3 个 SXSSF 流式导出 | 样式密集、复杂表头 | **低**——fesod 简化的是数据映射，复杂样式仍要手写 POI API；SXSSF 已解决大文件写入 |
| B. 表头扫描式导入 | `TrainExcelTemplateHeaderValidator`（扫描前 10 行定位表头，对齐 PHP 老格式）、`TrainSignUpPeopleImportService`（667 行，`WorkbookFactory` 全量加载 + 手动逐行解析） | 表头位置不固定、动态列映射 | **中**——fesod 注解模型默认假设固定表头行；此类需 `headRowNumber(0)` 拿原始行自行定位，映射优势打折，但流式读内存收益保留 |
| C. 纯数据导入导出 | 查证 Excel、竞赛名单、成绩单等（0 样式调用） | 固定表头 + 行数据 | **高**——注解模型最标准场景，可省 30-50% 代码 |
| D. Word 处理 | 7 个 XWPF 文件 | — | **不适用**（fesod 只管电子表格，继续 POI） |

## 3. 兼容性核对（结论：零障碍）

| 核对项 | 结果 |
|---|---|
| POI 版本 | fesod 2.0.2 依赖 POI **5.5.1**，与项目当前版本**完全一致**——无双版本冲突（项目已为此踩过坑，pom 注释可见） |
| 传递依赖 | `commons-csv 1.14.1`、`ehcache 3.9.11`、`slf4j-api 1.7.36`——项目均未显式使用，无冲突（ehcache 是 fesod/EasyExcel 多年标配，独立库不干扰 Spring Cache） |
| JDK | 项目 JDK 21，fesod 要求 Java 8+，兼容 |
| fesod-shaded | **不要引入**（内置依赖重打包，与项目现有 POI 类冲突） |

## 4. 评估结论

### 4.1 全量重构（38 个文件迁 fesod）——**不建议**

- 38 个文件是已稳定运行的业务功能（报名、查证、竞赛、认证、排班），重构的回归测试面大
- A 类（重样式）迁移后代码量几乎不减，B 类（表头扫描）还要绕过 fesod 的固定表头假设
- 收益主要在"代码可读性"，而现有手写代码已通过生产验证，动它纯属风险换美观

### 4.2 渐进引入——**建议采纳**

1. **pom 引入** `org.apache.fesod:fesod-sheet:2.0.2-incubating`（天然对齐 POI 5.5.1，无迁移即无风险——不引入时对现状零影响）
2. **新功能优先用 fesod**：新的导入导出直接注解模型写，享受流式读 + 映射省代码
3. **存量按需迁移**：只在触碰某个文件做需求改动时顺手迁移；纯稳定代码不动
4. **优先迁移候选**（收益最高的 C 类）：下次有需求的查证/名单/成绩类简单导入导出
5. **大文件导入**（如未来名单量级上万行）：fesod SAX 流式读是硬收益，新写一律 fesod，不要再用 `WorkbookFactory` 全量加载
6. **Word 的 7 个文件**：fesod 不覆盖，继续 POI

### 4.3 incubating 状态的风险

- 孵化期 API 理论上可变，但 fesod 的 API 血统是 EasyExcel 稳定多年的接口（read/write/listener 注解全同名），实际变动风险低
- 版本锁定 2.0.2-incubating + 关注 Apache 毕业进度即可；若未来毕业改 API，迁移点集中在入口调用（`FesodSheet.read/write`），面可控

## 5. 决策记录

| 项 | 决策 |
|---|---|
| 是否全量重构 | 否（2026-08-30 评估结论，待需求方确认） |
| 是否引入 fesod | 建议引入，新代码使用（待需求方确认后执行 pom 变更） |
| 存量 38 文件 | 保持现状，触碰时顺手迁移 |
| Word 7 文件 | 与本评估无关，继续 POI |
