// 管理端:题库主观审核指派
// 挂在"题库管理 > 任务管理 > 开发任务"下
// 菜单 code: task_examsub_audit_assign (per plan §3.2)

import { TaskAuditAssignTable } from './components/TaskAuditAssignTable';

export function TaskExamsubAuditAssignListPage() {
  return (
    <TaskAuditAssignTable
      taskType="examsub"
      permissionPrefix="task:examsub-audit"
      title="题库主观审核指派"
      detailColumnLabels={{
        zhishidian: '知识点',
        content: '题目内容',
        answer: '参考答案',
        isJisuan: '是否计算题',
      }}
    />
  );
}

export default TaskExamsubAuditAssignListPage;
