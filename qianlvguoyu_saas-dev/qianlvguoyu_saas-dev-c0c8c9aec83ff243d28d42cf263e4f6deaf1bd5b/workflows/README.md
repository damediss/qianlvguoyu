# workflows

放工作流和自动化配置。

## 子目录

| 目录 | 用途 |
| --- | --- |
| `n8n/` | n8n workflow 导出 JSON |
| `bpmn/` | Flowable / Camunda BPMN 流程文件 |
| `specs/` | 流程规则说明 |

核心业务状态应进入后端数据库，n8n 主要承担通知、定时、接口同步和低风险自动化。

