import { jsx as _jsx } from "react/jsx-runtime";
// 管理端:题库主观审核指派
// 挂在"题库管理 > 任务管理 > 开发任务"下
// 菜单 code: task_examsub_audit_assign (per plan §3.2)
import { TaskAuditAssignTable } from './components/TaskAuditAssignTable';
export function TaskExamsubAuditAssignListPage() {
    return (_jsx(TaskAuditAssignTable, { taskType: "examsub", permissionPrefix: "task:examsub-audit", title: "\u9898\u5E93\u4E3B\u89C2\u5BA1\u6838\u6307\u6D3E", detailColumnLabels: {
            zhishidian: '知识点',
            content: '题目内容',
            answer: '参考答案',
            isJisuan: '是否计算题',
        } }));
}
export default TaskExamsubAuditAssignListPage;
