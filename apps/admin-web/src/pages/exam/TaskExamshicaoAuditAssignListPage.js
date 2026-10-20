import { jsx as _jsx } from "react/jsx-runtime";
// 管理端:题库实操审核指派
// 挂在"题库管理 > 任务管理 > 开发任务"下
// 菜单 code: task_examshicao_audit_assign (per plan §3.2)
import { TaskAuditAssignTable } from './components/TaskAuditAssignTable';
export function TaskExamshicaoAuditAssignListPage() {
    return (_jsx(TaskAuditAssignTable, { taskType: "examshicao", permissionPrefix: "task:examshicao-audit", title: "\u9898\u5E93\u5B9E\u64CD\u5BA1\u6838\u6307\u6D3E", detailColumnLabels: {
            jianding: '考评点',
            content: '题目内容',
            pingfen: '评分',
            kaochang: '考场准备',
            kaosheng: '考生准备',
        } }));
}
export default TaskExamshicaoAuditAssignListPage;
