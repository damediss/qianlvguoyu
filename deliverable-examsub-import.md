VERDICT: PASS

# ExamTaskExamsub 导入端点 - 实施完成报告

## 概要
给 `ExamTaskExamsubController` 加了 `POST /api/exam/task-examsubs/import` 端点,
在 `ExamTaskExamsubApplicationService` 里实现了 `importExcel(MultipartFile, Integer, Integer)` 方法,
对应老 PHP `/yuntiku/upload?type=examsub` 的导入功能。

## 改动的文件

| # | 文件 | 改动 |
|---|------|------|
| 1 | `services/platform-api/src/main/java/com/guoyu/platform/exam/controller/ExamTaskExamsubController.java` | 加 `importExcel` 端点 + `MultipartFile` import |
| 2 | `services/platform-api/src/main/java/com/guoyu/platform/exam/service/ExamTaskExamsubApplicationService.java` | 加 `importExcel()` 方法 + 4 个 import helper + Apache POI / `Value` / `MultipartFile` 引用 |

## 端点签名

```
POST /api/exam/task-examsubs/import
@PreAuthorize("hasAuthority('task:examsub-audit:import')")
@AuditLog(module = "exam", action = "task-examsub-audit:import", targetType = "exam_task_examsub")
Content-Type: multipart/form-data

@RequestParam("file")     MultipartFile file
@RequestParam             Integer      workTypeId
@RequestParam             Integer      level

→ ApiResponse<Integer>  (导入成功条数)
```

## Service 方法签名

```java
@Transactional
public int importExcel(MultipartFile file, Integer workTypeId, Integer level)
```

## 实现要点 (跟 exam / examshicao 都不同)

1. **查重**: `exam_task_examsub` 表 WHERE `work_type_id=? AND level=?`,已存在则抛 `CONFLICT 409`
2. **存文件**: 用 `platform.cdn.path` 写到本地 CDN 目录,文件名 UUID + 原扩展名
3. **解析 Excel**: Apache POI `WorkbookFactory` + `DataFormatter`,5 列布局(每行一题):
   - A 列 (idx 0) = zhishidian (知识点)
   - B 列 (idx 1) = content (题目内容,空行视为终止,沿用 PHP `if(empty($row[1])) break`)
   - C 列 (idx 2) = answer (参考答案)
   - D 列 (idx 3) = isJisuan (是否计算题,0/1,空值或非数字文本默认 0)
   - E 列 (idx 4) = pic (图片 URL,**空字符串 → null,不入库 `""`**)
4. **长度校验**: zhishidian > 100 字符 或 content > 500 字符 → 400 + 清理已存文件
5. **写主任务**: `exam_task_examsub`,status=0,nums=有效行数,addtime=now
6. **写明细**: `exam_task_examsub_detail`,实际 INSERT 列 = `task_id, zhishidian, content, answer, isJisuan, pic` (5 列,不含 nanyicd/isblod,跟题面要求完全一致)
7. **失败回滚**: 任何解析 / 校验失败 → `delSilent(fullPath)` 删除已存文件 → 抛 `BusinessException`

## 编译 & 测试结果

```text
$ mvn clean test-compile -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml
[INFO] Compiling 739 source files with javac [debug parameters release 21] to target\classes
[INFO] Compiling 14 source files with javac [debug parameters release 21] to target\test-classes
[INFO] BUILD SUCCESS

$ mvn test -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml -Dtest=ExamTaskExamsubApplicationServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

7 个原有测试全部通过(没改 constructor,既有 mock 测不需要动)。

## 约束符合

- [x] 字段独立,跟 exam / examshicao 完全不同
- [x] isJisuan 默认 0,空白/无效文本也走 0
- [x] pic 空字符串 → null(不存 `""`)
- [x] 不新增依赖(Apache POI 已在 `ExamTaskXimuApplicationService` 引用过)
- [x] 权限用 `task:examsub-audit:import` (V20260605_001 迁移已建)
- [x] AuditLog module/action/targetType 跟同套菜单一致
- [x] 不改 service 构造器,既有测试不受影响

VERDICT: PASS
