VERDICT: PASS

# 实操题导入(examshicao)后端 - 交付

## Summary
仿照 ExamTaskXimuController/Service 的实现模式,给实操题审核任务加 `POST /api/exam/task-examshicaos/import` 端点
+ `importExcel` 服务方法。对应老 PHP `/yuntiku/upload?type=examshicao` → `examshicaoUpload()`。
Excel 解析逻辑按 PHP 老行为复刻(行 0 跳过、组头/续行分组、id>41 老数据查重),但用 `ObjectMapper` 把
`pingfen` 数组序列化成 JSON 写入 `exam_task_examshicao_detail.pingfen`。

## 改了哪些文件
1. `D:\workspace\qianlvguoyu_saas\services\platform-api\src\main\java\com\guoyu\platform\exam\controller\ExamTaskExamshicaoController.java`
   - 加 `import org.springframework.web.multipart.MultipartFile;`
   - 加 `POST /task-examshicaos/import` 端点
2. `D:\workspace\qianlvguoyu_saas\services\platform-api\src\main\java\com\guoyu\platform\exam\service\ExamTaskExamshicaoApplicationService.java`
   - 加 JSON/POI/文件 相关 import
   - 注入 `ObjectMapper`
   - 加 `@Value("${platform.cdn.path:./data/cdn}") String cdnPath`
   - 加 `importExcel(MultipartFile, Integer, Integer)` 方法
   - 加 `LEGACY_TASK_ID_LIMIT = 41` 常量
   - 加 import 辅助方法:`cell / parseIntOrNull / parseIntOrZero / buildPingfenJson / saveFile / delSilent`
3. `D:\workspace\qianlvguoyu_saas\services\platform-api\src\test\java\com\guoyu\platform\exam\service\ExamTaskExamshicaoApplicationServiceTest.java`
   - 构造器调用补 `Mockito.mock(ObjectMapper.class)`(预存测试必须随生产构造器签名同步)

## 端点签名
```
POST /api/exam/task-examshicaos/import
  @PreAuthorize("hasAuthority('task:examshicao-audit:import')")
  @AuditLog(module = "exam", action = "task-examshicao-audit:import", targetType = "exam_task_examshicao")
  Content-Type: multipart/form-data
  form-data: file (MultipartFile), workTypeId (Integer), level (Integer)
  200 → ApiResponse<Integer>  data = 导入的题目条数
```

## Service 方法签名
```java
@Transactional
public int importExcel(MultipartFile file, Integer workTypeId, Integer level)
```

## 编译结果
`mvn test-compile`: **BUILD SUCCESS**(739 main + 14 test 源文件全过)

`mvn test -Dtest=ExamTaskExamshicaoApplicationServiceTest`: **Tests run: 7, Failures: 0**

## 详见
引擎版 deliverable: `C:\Users\admin\.mavis\plans\plan_2bf52d25\outputs\examshicao-import-backend\deliverable.md`
