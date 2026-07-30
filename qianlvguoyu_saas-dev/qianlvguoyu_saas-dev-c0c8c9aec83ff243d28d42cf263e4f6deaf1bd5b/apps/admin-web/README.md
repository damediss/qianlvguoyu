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

## 约定

1. 知识库是平台内唯一 RAG 底座；企业、部门、课程只是同一套 RAG 下的知识空间。
2. 敏感资料上传入库时使用 `accessScope=restricted`，并配置签署人或高权限角色。
3. 前端只做展示控制，最终权限由 `platform-api` 裁决。
