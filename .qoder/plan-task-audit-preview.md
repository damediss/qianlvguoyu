# 题库审核指派页面 xlsx 文件预览功能

## Context

"题库理论审核指派"、"题库实操审核指派"、"题库主观审核指派"三个页面（共用 `TaskAuditAssignTable.tsx` 组件）目前缺少对导入的 xlsx 原始文件的页面预览功能。

对比"开发任务"（细目表）页面 `DevTaskDetailPage.tsx`，其已有"细目表文件"按钮，通过 Office Online Viewer iframe 预览 xlsx 文件。需要在三个审核指派页面添加同样的预览能力。

## Task 1: 后端 VO 添加 `fileUrl` 和 `fileType` 字段

三个 VO 结构完全相同，均只有 `filePath`，缺少 `fileUrl` 和 `fileType`。

**修改文件：**
- `services/.../exam/vo/ExamTaskExamVO.java`
- `services/.../exam/vo/ExamTaskExamshicaoVO.java`
- `services/.../exam/vo/ExamTaskExamsubVO.java`

**改动：** 每个 VO 添加 `fileUrl`（String）和 `fileType`（String）字段及 getter/setter。

## Task 2: 后端 Service `toVO()` 填充 `fileUrl`/`fileType`

参照 `ExamTaskXimuApplicationService` 的做法（第 180-190 行），在三个 service 中：

**修改文件：**
- `services/.../exam/service/ExamTaskExamApplicationService.java`
- `services/.../exam/service/ExamTaskExamshicaoApplicationService.java`
- `services/.../exam/service/ExamTaskExamsubApplicationService.java`

**改动：**
1. 添加 `@Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") private String cdnUrl;` 字段
2. 在 `toVO()` 方法中，`setFilePath()` 之后添加：
   ```java
   if (e.getFilePath() != null && !e.getFilePath().isEmpty()) {
     vo.setFileUrl(cdnUrl + "/" + e.getFilePath());
     String ext = e.getFilePath();
     int dotIdx = ext.lastIndexOf('.');
     if (dotIdx >= 0) {
       ext = ext.substring(dotIdx + 1).toLowerCase();
       if ("xls".equals(ext)) ext = "xlsx";
       vo.setFileType(ext);
     }
   }
   ```

> **注意：** `toVO()` 当前是 `static` 方法，需要改为实例方法以访问 `cdnUrl` 字段，调用处无需改动（已是实例调用）。

## Task 3: 前端 `TaskAuditAssignTable.tsx` 添加预览按钮和 Modal

**修改文件：** `apps/admin-web/src/pages/exam/components/TaskAuditAssignTable.tsx`

**改动：**
1. 导入 `EyeOutlined`、`DownloadOutlined` 图标
2. 添加文件预览状态：`fileOpen`、`fileUrl`、`fileDownUrl`
3. 添加 `handleShowFile` 函数：
   ```ts
   const handleShowFile = (row: TaskAuditVO) => {
     setCurrentRow(row);
     setFileUrl('https://view.officeapps.live.com/op/view.aspx?src=' + encodeURIComponent(row.fileUrl));
     setFileDownUrl(row.fileUrl);
     setFileOpen(true);
   };
   ```
4. 在操作列添加"预览"按钮（放在"明细"和"导出"之间）：
   ```tsx
   {r.fileUrl && (
     <Button size="small" icon={<EyeOutlined />} onClick={() => handleShowFile(r)}>
       预览
     </Button>
   )}
   ```
5. 添加文件预览 Modal（复用 DevTaskDetailPage 的结构）：
   ```tsx
   <Modal title="文件预览" open={fileOpen} onCancel={...} width="85%" footer={null}>
     <div style={{ marginBottom: 8 }}>
       <a href={fileDownUrl} target="_blank"><Button icon={<DownloadOutlined />} size="small">下载文件</Button></a>
     </div>
     <iframe src={fileUrl} style={{ width: '100%', height: 600, border: '1px solid #e0e0e0' }} />
   </Modal>
   ```

## 验证

1. 后端编译通过
2. 前端无 TypeScript 错误
3. 在任一审核指派页面列表中，点击"预览"按钮应打开 Office Online Viewer iframe 展示对应 xlsx 文件
4. 若 `fileUrl` 为空（无文件），预览按钮不显示
