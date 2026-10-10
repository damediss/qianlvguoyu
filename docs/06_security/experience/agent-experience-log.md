# Agent 安全检查经验日志

> 只追加、不改写。每期执行至少 1 条。重复经验标注 `dup:{日期}` 链接首条。
> 格式见《周安全检查手册》§6。

---

### 2026-08-30 | SEC-SQLI-01/02 | ZCode / GLM-5.3-Flash
- **发现/误报**：全项目 30 个 Java 文件含 `${` 字面量，初看吓人，逐一核对后全部是 `@Value("${...}")` 配置占位符，**无一例注解 SQL 注入**。
- **方法与坑**：区分误报的方法——先 `grep -rln -F '${'` 拿文件清单，再用 `grep -rn -F '${' | grep -iE '@Select|INSERT |WHERE|ORDER'` 收敛到 SQL 关键词附近；本项目 SQL 关键词附近 0 命中即可快速判 PASS。注意 grep 必须 `-F` 固定字符串，`'\${'` 在 BRE 里的转义在不同 grep 实现下行为不一致，曾导致同一命令一次命中一次不命中。
- **建议**：无需升级检查点；方法已写进手册 SEC-SQLI-02。

### 2026-08-30 | SEC-SQLI-04 | ZCode / GLM-5.3-Flash
- **发现/误报**：`Wrapper.last()` 全项目 20+ 处，绝大多数是常量（`LIMIT 1`/`FOR UPDATE`/`ORDER BY RAND()`），仅 2 处变量拼接且均为 `int`（`Math.min(size,50)`、固定值），非注入点。
- **方法与坑**：判定 `last("LIMIT " + x)` 是否安全只看两件事：x 的**声明类型**（int/long 即安全，String 即 FAIL）和 x 的**赋值来源**是否用户输入。`AppExamSmartService.java:744` 的 `s` 来自身上方法顶部 `int s = Math.min(size, 50)`，`size` 经 `intVal()` 提取——链路闭环才判 PASS，不能只看拼接行本身。
- **建议**：保持 SEC-SQLI-04 现状即可，判定标准里"数值类型 + 来源闭环"两点已写明。

### 2026-08-30 | SEC-CSRF-02/04 | ZCode / GLM-5.3-Flash
- **发现/误报**：CORS 与 JWT 密钥的问题**不在代码而在配置交付链**——代码层 dev 默认全开/弱默认值写死在 yml 里，生产是否覆盖只能看服务器 `.env`，本地仓库无法闭环判定。
- **方法与坑**：这类"配置兜底值"问题要拆两半报告：代码默认值（可判定 FAIL/风险）+ 生产实际值（标"待核查"而不是猜测）。顺带经验：`git grep` 前先确认目标分支，dev 与生产部署的 env 可能不同步。
- **建议**：待 SSH 凭据到位后把"生产 env 核查"固化为检查点 SEC-CONF-01（需服务器访问权限，作为手动/半自动项）。**已作为提案记录于 2026-W35 报告 §4。**

### 2026-08-30 | SEC-CONF（F-008 定级） | ZCode / GLM-5.3-Flash
- **发现/误报**：F-008 初判 P0 **定级过高**——需求方确认仓库私有、读者可信，主动泄露风险低，降级 P2。教训：密钥入库类发现的定级必须先问威胁模型（仓库可见范围、读者信任边界），不能一刀切按"凭证泄露=P0"。
- **方法与坑**：但降级时要核实两个技术事实并如实告知：① **共享资源的权限边界**——该 COS 密钥 dev/prod 共桶（仅 objectKeyPrefix 区分，`FileApplicationService.buildObjectKey`），"测试密钥"实际有生产对象读写权，定级讨论里必须把这个事实摆出来让决策者知情；② **替代机制是否已存在**——`.gitignore` 已忽略本地 env 文件、项目约定就是"真实值进 env、yml 留占位"（MYSQL_PASSWORD 同模式），所以"挪出 yml 会不会影响测试"是伪问题，本机测试体验零损失。
- **建议**：固化检查点 SEC-CONF-02 的判定规则：密钥类 yml 默认值——为空或占位符=PASS；真实凭证=标记 FAIL 但定级时结合仓库可见性访谈调整（私有+可信团队最高 P2）。

### 2026-08-30 | SEC-CSRF-03 | ZCode / GLM-5.3-Flash
- **发现/误报**：F-002 初判"无鉴权暴露"**风险高估**——整改核实发现 permitAll 只放行过滤器链，`TrainSignUpSelfController`/`TrainSignUpCollectionController` 方法上都有 `@PreAuthorize(train:sign-up:*)`，匿名请求实际 403；真正匿名可用的是学员扫码报名端点（SignUpRegisterTestController，无权限注解但业务就是匿名报名，身份证+手机号弱身份校验）。
- **方法与坑**：① 判断 Spring Security 白名单风险必须看三层：SecurityConfig 白名单 → 方法级 @PreAuthorize → Controller 内部身份校验，只看第一层会误判；② **注释会漂移**——"测试专用、生产移除"的注释写于开发期，后来该 Controller 已成生产匿名链路（手机端 sign-up-collect.js 调用），整改前先 grep 调用方确认生产依赖，否则一刀切加鉴权会打断学员报名；③ 改 authorizeHttpRequests 流式 DSL 为语句块时可注入 @Value 开关实现环境差异化白名单。
- **建议**：新增检查点 SEC-AUTHZ-02（白名单端点三层核查：白名单→注解→内部校验，并 grep 移动端/前端仓库确认真实调用方），已在 W35 报告 §4 提案。**另：整改过程中发现 F-008（COS 密钥明文入库，P0）**——经验：处理任何一个安全发现时顺带 grep yml 中其他密钥默认值，常有连带收获。
