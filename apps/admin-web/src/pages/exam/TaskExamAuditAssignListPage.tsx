// 管理端:题库理论审核指派
// 挂在"题库管理 > 任务管理 > 开发任务"下
// 菜单 code: task_exam_audit_assign (per plan §3.2)

import { TaskAuditAssignTable } from './components/TaskAuditAssignTable';

export function TaskExamAuditAssignListPage() {
  return (
    <TaskAuditAssignTable
      taskType="exam"
      permissionPrefix="task:exam-audit"
      title="题库理论审核指派"
      detailColumnLabels={{
        examType: '题型',
        examContent: '题干',
        examAnswer: '参考答案',
        isblod: '重点审核',
      }}
    />
  );
}

export default TaskExamAuditAssignListPage;
