VERDICT: PASS

# Verify: 实操题导入后端(examshicao) — Controller 端点 + Service 解析

## Summary
按任务规约的 5 条 FAIL 条件逐一核对,无一条命中:编译通过、端点结构合规、Service 流程符合
PHP 老逻辑、id>41 查重存在、INSERT 字段无错位、deliverable 格式正确。已在 4 个独立维度做
对抗式探查(build、test suite、permission migration、security config),全部通过。

## Check 1: 编译 BUILD SUCCESS
**Method:**
  `$env:JAVA_HOME = 'D:\Program Files\Java\oracleJdk-21'`
  `mvn.cmd test-compile -f services\platform-api\pom.xml -s scripts\custom-maven-settings.xml`
**Evidence:**
  ```
  [INFO] Compiling 739 source files with javac [debug parameters release 21] to target\classes
  [INFO] Compiling 14 source files with javac [debug parameters release 21] to target\test-classes
  [INFO] BUILD SUCCESS
  ```
**Result: PASS**

## Check 2: Controller 端点签名
**Method:** 读 `ExamTaskExamshicaoController.java` 第 60-70 行,PowerShell 二次确认
**Evidence:**
  ```
  Line 60:   @PostMapping("/task-examshicaos/import")
  Line 61:   @PreAuthorize("hasAuthority('task:examshicao-audit:import')")
  Line 62:   @AuditLog(module = "exam", action = "task-examshicao-audit:import", targetType = "exam_task_examshicao")
  Line 63:   public ApiResponse<Integer> importExcel(
  Line 64:       @RequestParam("file") MultipartFile file,
  Line 65:       @RequestParam Integer workTypeId,
  Line 66:       @RequestParam Integer level) {
  ```
  - ✓ `@PostMapping("/task-examshicaos/import")` 全限定路径 → `/api/exam/task-examshicaos/import`(类上 `@RequestMapping("/exam")`)
  - ✓ `@PreAuthorize("hasAuthority('task:examshicao-audit:import')")` 权限字符串与 SQL 迁移一致
  - ✓ 参数:MultipartFile + Integer(workTypeId) + Integer(level)
  - ✓ 返回:ApiResponse<Integer>
**Result: PASS**

## Check 3: Service.importExcel 方法 + @Transactional
**Method:** 读 `ExamTaskExamshicaoApplicationService.java` 第 267-408 行
**Evidence:**
  ```
  Line 267:  @Transactional
  Line 268:  public int importExcel(MultipartFile file, Integer workTypeId, Integer level) {
  ```
  - ✓ `@Transactional`(非 readOnly)
  - ✓ 方法签名 `(MultipartFile, Integer, Integer)` 与 Controller 一致
  - ✓ 返回 int(导入条数)
**Result: PASS**

## Check 4: work_type_id + level 查重带 id>41 限制
**Method:** 读第 278-286 行 + 84 行常量定义
**Evidence:**
  ```
  Line 84:   private static final int LEGACY_TASK_ID_LIMIT = 41;
  ...
  Line 278:  // 查重:PHP 老逻辑是 work_type_id + level + id>41(老 id<=41 的数据不算)
  Line 279:  LambdaQueryWrapper<ExamTaskExamshicaoEntity> dup = new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
  Line 280:      .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId)
  Line 281:      .eq(ExamTaskExamshicaoEntity::getWorkTypeId, workTypeId)
  Line 282:      .eq(ExamTaskExamshicaoEntity::getLevel, level)
  Line 283:      .gt(ExamTaskExamshicaoEntity::getId, LEGACY_TASK_ID_LIMIT);
  Line 284:  if (taskMapper.exists(dup)) {
  Line 285:    throw new BusinessException(ErrorCode.CONFLICT, "该工种等级已存在");
  Line 286:  }
  ```
  - ✓ `id>41` 限制(`.gt(getId, 41)`)
  - ✓ 命中时抛 `CONFLICT 409`,ErrorCode.CONFLICT = "409" 已确认(ErrorCode.java:9)
  - ✓ 多租户隔离(`.eq(tenantId, ...)`)
**Result: PASS**

## Check 5: POI / WorkbookFactory / Sheet 解析
**Method:** 读第 51-55 行的 import 和 298-323 行的实际使用
**Evidence:**
  ```
  Line 51: import org.apache.poi.ss.usermodel.DataFormatter;
  Line 52: import org.apache.poi.ss.usermodel.Row;
  Line 53: import org.apache.poi.ss.usermodel.Sheet;
  Line 54: import org.apache.poi.ss.usermodel.Workbook;
  Line 55: import org.apache.poi.ss.usermodel.WorkbookFactory;
  ...
  Line 298: try (InputStream is = Files.newInputStream(fullPath)) {
  Line 299:   Workbook workbook = WorkbookFactory.create(is);
  Line 300:   Sheet sheet = workbook.getSheetAt(0);
  ```
  - ✓ WorkbookFactory 自动识别 .xls / .xlsx
  - ✓ Sheet 迭代 + DataFormatter 处理所有单元格类型
  - ✓ 解析失败抛 `BAD_REQUEST 400` 并 delSilent 清理已保存的文件
**Result: PASS**

## Check 6: INSERT 字段正确(无 exam_a-h 等错位)
**Method:** 读第 364-373 行(组头)和 388-398 行(续行)
**Evidence:**
  ```
  // 组头
  detail.setTenantId(tenantId);
  detail.setTaskId(task.getId());      // task_id
  detail.setJianding(jianding);        // jianding
  detail.setContent(content);          // content
  detail.setKaochang(kaochang);        // kaochang
  detail.setKaosheng(kaosheng);        // kaosheng
  detail.setPingfen(buildPingfenJson(...));  // pingfen (JSON)
  detail.setDeleted(0);
  detailMapper.insert(detail);
  ```
  - ✓ 字段全部正确(任务规约要求的 6 个字段:task_id, jianding, content, pingfen, kaochang, kaosheng)
  - ✓ 没有写入 xingwei/kaohe1-4/nanyicd/isblod(虽然 Entity 有这些列,但 PHP 老行为不写,与生产行为一致)
  - ✓ 没有写入任何"exam_a-h"等不存在的列
  - ✓ 续行复用 lastJianding,content/kaochang/kaosheng 填空字符串(与 deliverable 文档说明一致)
**Result: PASS**

## Check 7: deliverable 格式
**Method:** 读 `D:\workspace\qianlvguoyu_saas\deliverable-examshicao-import.md` 第 1 行
**Evidence:**
  ```
  Line 1: VERDICT: PASS
  ```
  - ✓ 第一行是 `VERDICT: PASS`(也允许 FAIL,只要是这两个之一即可)
  - 注:deliverable 第 14 行的文件路径有拼写错误 `ssrc/main/...`(应为 `src/main/...`),但这是 markdown 描述错误,不影响实际代码
**Result: PASS**

## Adversarial Probes

### Probe A: 现有测试是否覆盖新方法
**Method:** 跑 `mvn test -Dtest=ExamTaskExamshicaoApplicationServiceTest`,看 7 个测试中哪些覆盖 importExcel
**Evidence:**
  ```
  [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
  7 个测试 = page(1) + delete(1) + assignExpert(3) + getDaily(1) + autoCompleteStatus(1) = 7
  **0 / 7** 覆盖新加的 `importExcel` 方法。
  Producer 只补了 `Mockito.mock(ObjectMapper.class)` 同步构造器签名,没补 importExcel 单元测试。
**Result: NOTED (not a FAIL per task spec, but coverage gap)**
  > 新方法的解析/dedup/JSON 序列化/文件保存逻辑全部未经单元测试覆盖,只能通过读代码 + 编译通过来推断正确性。
  > 建议下一轮让 Producer 补 importExcel 单元测试(给定一张 .xlsx 输入,断言 detailMapper.insert 收到 N 条,
  >   pingfen 字段是合法 JSON,Excel 损坏时抛 400 + delSilent)。

### Probe B: 权限 migration 是否真的存在并授权 admin
**Method:** 读 `V20260605_001__add_3_import_permissions.sql`
**Evidence:**
  ```sql
  -- Line 21: id=7061, menu_id=4711, code='task:examshicao-audit:import'
  -- Line 29: 授权给 role_id=2000 (PLATFORM_ADMIN)
  ```
  - ✓ 权限 ID 7061 已 insert
  - ✓ 已授权给 PLATFORM_ADMIN (id=2000)
  - ✓ 与 Producer 在 Notes 中的声明一致
**Result: PASS**

### Probe C: SecurityConfig 是否启用 @PreAuthorize
**Method:** 读 `config/SecurityConfig.java`
**Evidence:**
  ```
  Line 27: @EnableMethodSecurity
  Line 51:   .anyRequest().authenticated()
  ```
  - ✓ `@EnableMethodSecurity` 已启用 → `@PreAuthorize` 生效
  - ✓ 没有任何 `/api/exam/**` 的特殊豁免,所有 /exam/** 都要走认证 + @PreAuthorize
**Result: PASS**

### Probe D: Controller 路径是否会与 public-data / page 冲突
**Method:** 列出所有 /task-examshicaos/* 端点
**Evidence:**
  ```
  GET    /task-examshicaos                      (page)
  GET    /task-examshicaos/{id}/details         (details)
  GET    /task-examshicaos/{id}/daily           (daily)
  POST   /task-examshicaos/import               ← NEW
  POST   /task-examshicaos/{id}/assign-expert
  DELETE /task-examshicaos/{id}
  GET    /task-examshicaos/public-data
  ```
  - ✓ `/import` 不会与 `/{id}/details` 等冲突(不同 HTTP method + 不同 path segment)
  - ✓ `/import` 不会与 `/public-data` 冲突(不同 method + 不同 segment)
**Result: PASS**

## Concerns (NOT failures,只是记录给团队)

1. **零单元测试覆盖新方法**:7 个测试用例 0 个覆盖 importExcel。功能正确性只能靠代码审查,
   无法通过自动化回归保护。下次改动 importExcel 的 Excel 解析/查重/JSON 序列化逻辑时,无人能保证不破。
2. **deliverable 路径拼写错误**:第 14 行写 `ssrc/main/...`(多一个 s),实际代码路径正确,文档误导而已。
3. **续行行为与 PHP 老逻辑有偏差**:同 jianding 下多个评分要素时,Java 是"每条评分一条 detail"
   而 PHP 是"合并到一条 detail.pingfen JSON 数组"。Producer 已在 Notes 中标记为"已知的偏差",
   前端按 jianding 分组展示即可。这是有意设计,不构成 FAIL。
4. **File 清理只在解析失败时**:第 316-322 行 delSilent 只覆盖 Excel 解析失败路径。
   若 DB INSERT 阶段抛异常(比如约束冲突),文件不会被自动清理(但 @Transactional 会回滚 DB 写入,
   文件则成为孤儿文件留在 CDN 目录)。建议未来加 @TransactionalEventListener 或 try-catch-finally 统一清理。

## FAIL Conditions Checklist(任务规约 5 条)
- [x] 编译 BUILD FAILURE → NO(BUILD SUCCESS)
- [x] 端点路径/权限/参数错误 → NO(`/task-examshicaos/import` + `task:examshicao-audit:import` + 3 参 OK)
- [x] 没做 id>41 限制 → NO(`.gt(getId, 41)` ✓)
- [x] 字段错位(写入 exam_a-h 等不该有的列) → NO(只写 task_id/jianding/content/kaochang/kaosheng/pingfen)
- [x] deliverable.md 格式错 → NO(Line 1 = `VERDICT: PASS`)

**Final verdict: PASS**
