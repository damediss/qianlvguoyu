VERDICT: PASS

# exam-import-backend 交付

## Summary
给 `ExamTaskExamController` 加了 `POST /api/exam/task-exams/import` 端点,并在 `ExamTaskExamApplicationService` 实现 Apache POI 解析 + 落库逻辑,沿用 `ExamTaskXimuApplicationService` 模式 (saveFile → WorkbookFactory → insert task + insert details),对应老 PHP `YuntikuController::examUpload` 的功能。

## Changed files
1. `D:\workspace\qianlvguoyu_saas\services\platform-api\src\main\java\com\guoyu\platform\exam\controller\ExamTaskExamController.java`
   - 新增 `import org.springframework.web.multipart.MultipartFile`
   - 新增端点 `@PostMapping("/task-exams/import")`

2. `D:\workspace\qianlvguoyu_saas\services\platform-api\src\main\java\com\guoyu\platform\exam\service\ExamTaskExamApplicationService.java`
   - 新增 POI / NIO / MultipartFile 等 import
   - 新增字段 `@Value("${platform.cdn.path:./data/cdn}") private String cdnPath;` (不破坏现有 9 参构造器,避免单测构造失败)
   - 新增方法 `public int importExcel(MultipartFile file, Integer workTypeId, Integer level)`,加 `@Transactional`
   - 新增私有 helper:`cell()`、`parseIntOrNull()`、`parseIntOrZero()`、`saveFile()`、`delSilent()`

## 端点签名
```
POST /api/exam/task-exams/import
权限: hasAuthority('task:exam-audit:import')
审计: module=exam, action=task-exam-audit:import, targetType=exam_task_exam
入参: 
  - file:        MultipartFile
  - workTypeId:  Integer
  - level:       Integer
出参: ApiResponse<Integer>   // 导入条数
```

## service 方法签名
```java
@Transactional
public int importExcel(MultipartFile file, Integer workTypeId, Integer level)
```

## 解析流程
1. 取 `tenantId` from `SecurityContext.currentUser().getTenantId()`
2. 校验 `workTypeId` / `level` 非 0,否则抛 `BusinessException(BAD_REQUEST, "请选择工种和等级")`
3. 查 `exam_task_exam` 同 `(tenant_id, work_type_id, level)` 是否已存在,存在则抛 `BusinessException(CONFLICT, "该工种等级已存在")`
4. `saveFile(file, tenantId)` → `${platform.cdn.path}/<uuid>.<ext>`
5. POI `WorkbookFactory.create(InputStream)` 读第一张 sheet,逐 cell 用 `DataFormatter` 提取
6. 失败时 `delSilent` 删已上传文件再 rethrow
7. 插入主任务 `exam_task_exam` (status=0, addtime=now, file_path=已存)
8. 逐行插入 `exam_task_exam_detail`,字段映射(对应 PHP `$el[0..19]`):
   - 0:xingwei, 1:kaohe1, 2:kaohe2, 3:kaohe3, 4:kaohe4, 5:jianding,
     6:level(Integer), 7:nanyicd, 8:examType, 9:examContent,
     10:examA, 11:examB, 12:examC, 13:examD, 14:examE,
     15:examF, 16:examG, 17:examH, 18:examAnswer,
     19:isblod (= `parseInt(col 19) > 0 ? 1 : 0`,匹配 PHP `$el[19]>0?1:0`)
9. break 条件:行 `xingwei(col 0)` 为空(同 PHP `if(empty($el[0])) break;`)
10. 写完更新主任务 `nums = count`
11. 返回 count

## 编译结果

```bash
$env:JAVA_HOME = 'D:\Program Files\Java\oracleJdk-21'
& "C:\Program Files\Java\apache-maven-3.9.11\bin\mvn.cmd" test-compile -f services\platform-api\pom.xml -s scripts\custom-maven-settings.xml
```

**结果:`BUILD SUCCESS`** (在快照 mvn 完整 output:739 个 main 源 + 14 个 test 源全部编译通过)

## 重要约束遵守
- ✅ 复用 Apache POI (无新依赖)
- ✅ 复用 `ExamTaskExamEntity` / `ExamTaskExamDetailEntity` / `ExamTaskExamMapper` / `ExamTaskExamDetailMapper`,无新建
- ✅ `@Value` 字段注入 `cdnPath`,保持现有 9 参构造器 → `ExamTaskExamApplicationServiceTest` 仍可编译
- ✅ `tenantId` 从 `SecurityContext.currentUser().getTenantId()` 取,多租户由 MyBatis-Plus `TenantLineInnerInterceptor` 自动拼 tenant_id
- ✅ 加 `@Transactional` (等价于 readOnly=false)

## Notes (verifier 注意)
- 这次运行 `mvn test-compile` 时,workspace 中同时有 sibling 任务的 in-flight 编辑(`ExamTaskExamshicaoController` / `ExamTaskExamshicaoApplicationService` / `ExamTaskExamsubController` / `ExamTaskExamsubApplicationService`),sibling 期间 `mvn test` 阶段会出现 `ExamTaskExamshicaoApplicationServiceTest` 构造器签名不匹配错误。这与本任务无关 — 我没动 examshicao/examsub 的代码。本任务的两处文件已干净编译通过。
- `ExamTaskExamApplicationServiceTest` 本身存在 pre-existing `NoClassDefFoundError: com/guoyu/platform/security/SecurityContext` (unrelated to my changes — git stash 验证过 baseline 也同样 fail)。这是测试 bootstrap 问题,不是 importExcel 引入的回归。
- `importExcel` 复用 ximu 模式:`saveFile` / `delSilent` / POI 解析 — 行为与 PHP `examUpload` 等价。
- 没建 `exam_task_expert` 行 — PHP 老的 `examUpload` 也没建,专家分配走单独 `/task-exams/{id}/assign-expert` 端点。
- isblod 严格按 PHP `>0?1:0`,非空非0 → 1,其它 → 0。
- 没复制 PHP 的 `iserror_0..8` 数据完整性校验(任务描述里没要求,且 ximu 也没做这部分 — 由前端/调用方负责模板正确性)。如需补,加在循环插入前即可。
