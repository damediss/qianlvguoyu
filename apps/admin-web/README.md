# admin-web

千铝国昱云平台 PC 管理后台，采用 React + Vite + TypeScript + Ant Design。

## 本地启动

```bash
npm install
npm run dev
```

默认开发地址：

```text
http://localhost:5173
```

API 代理到本地后端：

```text
http://localhost:8080/api
```

## 当前页面

1. `/knowledge/spaces`：企业 RAG 知识库管理、资料入库、检索问答草稿。
2. `/competition/events`：赛事列表。表格最小宽度 1150px，编码、范围、状态、时间及操作列固定宽度，名称使用剩余宽度；窄屏横向滚动时操作固定在右侧。
## 赛事列表布局验证

执行 `npm run build` 完成类型检查与构建。页面冒烟使用超长赛事名称和编码：名称最多显示两行、编码单行省略，悬浮显示完整内容；范围和状态不换行，开始/结束时间以 `YYYY-MM-DD HH:mm` 单行显示。分别在宽屏和窄屏确认名称及“详情”仍可打开赛事详情。

此布局调整沿用现有启动方式、API 代理和后端健康检查 `/api/actuator/health`，无需新增环境变量。

## 约定

1. 知识库是平台内唯一 RAG 底座；企业、部门、课程只是同一套 RAG 下的知识空间。
2. 敏感资料上传入库时使用 `accessScope=restricted`，并配置签署人或高权限角色。
3. 前端只做展示控制，最终权限由 `platform-api` 裁决。
