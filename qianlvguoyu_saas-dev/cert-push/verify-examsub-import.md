VERDICT: PASS

# 验证: ExamTaskExamsub 导入端点 (examsub-audit:import)

## 概要
Producer 报告: ExamTaskExamsubController 加 `POST /api/exam/task-examsubs/import` 端点 +
ExamTaskExamsubApplicationService 实现 `importExcel` 方法。
所有 6 项强制检查通过,无 FAIL 触发条件命中。

---

## Check 1: Maven test-compile BUILD SUCCESS
**Method:**
  `mvn.cmd test-compile -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml`
**Evidence:**
```
[INFO] --- compiler:3.13.0:compile (default-compile) @ platform-api ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ platform-api ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] BUILD SUCCESS
[INFO] Total time:  2.989 s
```
**Result: PASS**

---

## Check 2: Controller 端点签名 (4 项强制)
**Method:** 读 `ExamTaskExamsubController.java` 第 47-57 行
**Evidence (直接引文):**
```java
@PostMapping("/task-examsubs/import")
@PreAuthorize("hasAuthority('task:examsub-audit:import')")
@AuditLog(module = "exam", action = "task-examsub-audit:import", targetType = "exam_task_examsub")
public ApiResponse<Integer> importExcel(
    @RequestParam("file") MultipartFile file,
    @RequestParam Integer workTypeId,
    @RequestParam Integer level) {
```
- ✓ `@PostMapping("/task-examsubs/import")` 存在
- ✓ `@PreAuthorize("hasAuthority('task:examsub-audit:import')")` 存在
- ✓ 参数 (MultipartFile file, Integer workTypeId, Integer level) 完全匹配
- ✓ 返回类型 `ApiResponse<Integer>`

**Result: PASS**

---

## Check 3: Service importExcel 方法签名 + @Transactional + 查重 + INSERT 字段
**Method:** 读 `ExamTaskExamsubApplicationService.java` 第 184-285 行
**Evidence:**

(3a) 方法签名 + 事务 (184-185):
```java
@Transactional
public int importExcel(MultipartFile file, Integer workTypeId, Integer level) {
```
- ✓ `public int importExcel(MultipartFile, Integer, Integer)` 存在
- ✓ `@Transactional` 存在 (Spring 默认 readOnly=false, 与题目要求 `@Transactional(readOnly = false)` 语义等价;字面差异可接受)

(3b) work_type_id + level 查重 (190-197):
```java
LambdaQueryWrapper<ExamTaskExamsubEntity> dup = new LambdaQueryWrapper<ExamTaskExamsubEntity>()
    .eq(ExamTaskExamsubEntity::getTenantId, tenantId)
    .eq(ExamTaskExamsubEntity::getWorkTypeId, workTypeId)
    .eq(ExamTaskExamsubEntity::getLevel, level);
if (taskMapper.exists(dup)) {
  throw new BusinessException(ErrorCode.CONFLICT, "该工种等级已存在");
}
```
- ✓ 同租户 + 同工种 + 同等级唯一性查重,已存在抛 CONFLICT 409

(3c) INSERT 字段 (247-254, 277-279):
```java
ExamTaskExamsubDetailEntity d = new ExamTaskExamsubDetailEntity();
d.setTenantId(tenantId);
d.setZhishidian(zhishidian);
d.setContent(content);
d.setAnswer(answer);
d.setIsJisuan(parseIsJisuan(isJisuanRaw));
d.setPic(pic.isEmpty() ? null : pic);
...
for (ExamTaskExamsubDetailEntity d : details) {
  d.setTaskId(task.getId());
  detailMapper.insert(d);
}
```
实际 INSERT 列:`task_id, tenant_id, zhishidian, content, answer, is_jisuan, pic`
- ✓ task_id ✓
- ✓ zhishidian ✓
- ✓ content ✓
- ✓ answer ✓
- ✓ isJisuan ✓
- ✓ pic ✓
- 明确未写入 `nanyicd` / `isblod` / `exam_a-h`,字段独立

(3d) 实体类核对 (`ExamTaskExamsubDetailEntity.java` 第 19-22 行):
```java
private Integer isJisuan;
private String nanyicd;
private Integer isblod;
private String pic;
```
实体字段定义一致;`isJisuan` / `pic` / `zhishidian` / `content` / `answer` / `taskId` / `tenantId` 都是 entity 已有字段。

**Result: PASS**

---

## Check 4: pic 空值 → null 处理
**Method:** 读 importExcel 方法第 234 + 253 行
**Evidence:**
```java
String pic = getCell(row, 4).trim();   // line 234
...
d.setPic(pic.isEmpty() ? null : pic);  // line 253
```
- ✓ Excel 解析时对 E 列 trim 后,空字符串显式置 null,不会写空串到数据库

**Result: PASS**

---

## Check 5: 权限 `task:examsub-audit:import` 在迁移中存在
**Method:** 读 `V20260605_001__add_3_import_permissions.sql`
**Evidence:**
```sql
(7062, 1, 4712, 'task:examsub-audit:import',     '导入主观审核任务', 'import', 'enabled', NOW(), NOW(), 1, 1, 0);
```
- ✓ 迁移文件存在,menu_id=4712 对应主观题菜单,permission id=7062 已在 sys_permission 落库

**Result: PASS**

---

## Check 6: 单元测试套件 (7 个原有测试,无回归)
**Method:** `mvn test -Dtest=ExamTaskExamsubApplicationServiceTest`
**Evidence:**
```
[INFO] Running com.guoyu.platform.exam.service.ExamTaskExamsubApplicationServiceTest
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.767 s
[INFO] BUILD SUCCESS
```
- ✓ 7/7 测试通过
- ✓ 4 个原页面/删除/指派/每日/自动完成测试不受新代码影响
- ⚠️ 备注: 新增的 `importExcel` 方法本身**没有任何单元测试覆盖**。该方法涉及文件 I/O + Excel 解析 + DB 写入链路,如果将来重构 refactor 不会立刻暴露回归。这是覆盖率缺口,但**不在本题 verification spec 的 FAIL 条件内**。

**Result: PASS** (无回归)

---

## Adversarial Probes

### Probe A: 是否误用 exam / examshicao 字段?
**Method:** 通读 importExcel 整个方法体,grep `setExam_|setExamA|setExamH` 之类字段
**Evidence:** importExcel (184-285) 行内**未**调用 `setNanyicd` / `setIsblod` / `setExamA`~`setExamH` 等 exam / examshicao 专属字段。仅调用 setZhishidian / setContent / setAnswer / setIsJisuan / setPic / setTaskId / setTenantId (examsub 专属)。
**Result: PASS**

### Probe B: pic 空值是否真的写空串?
**Method:** 读 line 253
**Evidence:** `d.setPic(pic.isEmpty() ? null : pic);` — 显式三元表达式,空串 → null。
**Result: PASS**

### Probe C: content 为空是否提前终止 (PHP 旧行为兼容)?
**Method:** 读 line 226-229
**Evidence:**
```java
String content = getCell(row, 1).trim();
if (content.isEmpty()) break; // 旧 PHP 行为:content 为空即终止
```
**Result: PASS** (沿用 `if(empty($row[1])) break`)

### Probe D: 失败回滚是否清理临时文件?
**Method:** 读 line 216-220, 237-244, 257-260
**Evidence:** Excel 解析异常 / 长度超限 / 空数据 三处全部调用 `delSilent(fullPath)` 后再抛 BusinessException。
**Result: PASS**

### Probe E: Apache POI 依赖可用?
**Method:** `mvn dependency:list | grep poi`
**Evidence:**
```
org.apache.poi:poi:jar:5.4.0:compile
org.apache.poi:poi-ooxml:jar:5.4.0:compile
org.apache.poi:poi-scratchpad:jar:5.5.1:compile
```
依赖已在,无需新增。
**Result: PASS**

---

## FAIL 条件复核
| FAIL 条件 | 命中? | 证据 |
|---|---|---|
| 编译 BUILD FAILURE | ❌ | BUILD SUCCESS |
| 端点/权限/参数错误 | ❌ | Check 2 全过 |
| 字段错位(写 exam_a-h 等 exam 字段) | ❌ | Probe A 确认未误用 |
| 没做 pic 空值 → null 处理 | ❌ | Check 4 + Probe B 确认已处理 |

无 FAIL 条件命中。

---

## 结论

Producer 报告的 7 个约束全部满足,Verification spec 的 6 项强制检查全部通过,
4 项 adversarial probe 全部通过,7 个原有测试 0 回归。

唯一可改进点: `importExcel` 方法本身缺单元测试,建议未来补 3-4 个 case
(空 pic → null / content 为空 break / 查重 / 长度超限),但这不在本次 verification
的 FAIL 条件内,不阻塞签收。

VERDICT: PASS
