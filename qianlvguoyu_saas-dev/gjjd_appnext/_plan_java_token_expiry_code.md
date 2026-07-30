# 待确认：Java 竞赛后端 token 过期响应码

## 背景

`utils/auth.js` 中的 `isLoginExpired()` 是统一判断接口是否因登录态失效而需要静默重登的函数。该函数被两套 request 共同使用：

- `utils/index.js` → PHP 主后端
- `utils/java.js` → Java 竞赛后端

当前实现（合并后）：

```js
export function isLoginExpired(data) {
  return data && data.code === -1 && /登陆过期|登录过期/.test(data.msg || '');
}
```

判定条件：**必须同时满足 `code === -1` 且 `msg` 包含 "登陆过期" / "登录过期"**。

## 已确认的后端行为

### PHP 主后端

```php
$this->showjson(-1,'登陆过期');
```

- 过期码固定为 `code: -1`
- 提示消息固定为 `"登陆过期"`
- 当前 `isLoginExpired()` 可正确拦截

## 待确认的问题

### Java 竞赛后端

根据 `utils/response.js`：

```js
export const RESPONSE_CODE = {
  SUCCESS: 1,  // 成功
  ERROR: 0     // 失败
}
```

已知 Java 接口成功码为 `1`，失败通用码为 `0`。但 **token 过期 / 未登录时具体返回什么 `code` 和 `msg`**，前端代码中没有找到明确依据。

如果 Java 后端在 token 过期时返回：

| 假设 | 示例响应 | 当前 `isLoginExpired()` 是否能拦截 |
|---|---|---|
| A. 沿用 `code: 0` + 特定 msg | `{code: 0, msg: '登录过期', data: null}` | ❌ 不拦截（因为要求 code === -1） |
| B. 使用 `code: -1` + msg | `{code: -1, msg: '登录过期', data: null}` | ✅ 可拦截 |
| C. 使用 `code: 401` + msg | `{code: 401, msg: '登录已过期', data: null}` | ❌ 不拦截 |
| D. 使用 `code: 10001` + msg | `{code: 10001, msg: 'token无效', data: null}` | ❌ 不拦截 |

若 Java 后端采用 A/C/D 中的任何一种，当前 `isLoginExpired()` 不会触发静默重登，竞赛接口将出现与 PHP 接口不一致的体验：token 过期后不会自动刷新，而是直接把错误抛给业务层。

## 影响范围

- `utils/java.js` 的 `request` 函数响应拦截
- 所有 Java 竞赛接口（`api/competition.js` 中 `/appcompetition/*` 系列）的 token 过期场景
- 若用户正在填写竞赛报名/成绩等表单，token 过期后无法自动恢复，可能丢失数据

## 建议确认事项

请 Java 后端同事确认以下问题，并在本文件记录结论：

1. Java 竞赛后端 token 过期时返回的 `code` 是多少？（例如：`-1` / `0` / `401` / `10001` / 其他）
2. 返回的 `msg` 文案是什么？是否包含以下任一关键词：
   - 登录过期 / 登陆过期
   - 登录失效
   - 请重新登录
   - 未登录
   - token 无效 / token 已失效
   - 登录超时
   - 请先登录
3. 是否区分：
   - token 自然过期
   - 未登录（请求未带 token）
   - 被踢下线 / 单点登录

## 当前处理策略（保守做法）

在得到 Java 后端确认前，保持 `code === -1` 的严格判定，避免把普通业务错误（如 `code: 0` 的"参数错误"）误判为登录过期而触发自动重登。

## 待执行动作

1. 拿到 Java 后端 token 过期响应格式后，更新 `utils/auth.js` 的 `isLoginExpired()`。
2. 同步测试：
   - 在竞赛相关页面触发 token 过期，确认是否能自动静默重登
   - 验证不会因为普通业务错误触发无意义的跳转登录
3. 如果两套后端过期码不一致，考虑在 `isLoginExpired()` 中分别支持，或让 Java 后端统一返回 `code: -1`（与 PHP 一致，改造成本最低）。

## 相关文件

- `utils/auth.js` → `isLoginExpired()`
- `utils/index.js` → PHP 主后端 request
- `utils/java.js` → Java 竞赛后端 request
- `utils/response.js` → Java 响应码常量
- `store/competition.js` → Java 接口状态管理

## 结论记录

> 待填写：
> - Java 后端 token 过期 `code`：______
> - Java 后端 token 过期 `msg`：______
> - 是否需要修改前端 `isLoginExpired()`：是 / 否
> - 修改后合并日期：______
