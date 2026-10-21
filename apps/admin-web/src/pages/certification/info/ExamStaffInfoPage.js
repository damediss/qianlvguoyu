import { jsx as _jsx } from "react/jsx-runtime";
import { Tag } from 'antd';
import { createExamStaffInfo, deleteExamStaffInfo, disableExamStaffInfo, enableExamStaffInfo, pageExamStaffInfo, reviewExamStaffInfo, submitExamStaffInfo, updateExamStaffInfo, } from '../../../api/certificationInfo';
import { InfoManagementTable } from './InfoManagementTable';
const columns = [
    { title: '姓名', dataIndex: 'staffName', width: 140 },
    { title: '认定管理员账号', dataIndex: 'accountUsername', width: 170, render: (value) => value || '-' },
    {
        title: '任务编号',
        dataIndex: 'assignedTaskNos',
        width: 180,
        ellipsis: true,
        render: (value) => value || '-',
    },
    { title: '在岗状态', dataIndex: 'workStatus', width: 130, render: (value) => _jsx(Tag, { children: statusLabel(value) }) },
    { title: '联系电话', dataIndex: 'maskedPhone', width: 160, render: (value) => value || '-' },
];
export function ExamStaffInfoPage() {
    return (_jsx(InfoManagementTable, { title: "\u8003\u52A1\u4FE1\u606F", queryKey: "certification-info-exam-staff", permissionPrefix: "certification.info.exam-staff", rowKey: "id", statusField: "staffName", columns: columns, auditColumnIndex: 2, fields: [
            { name: 'staffName', label: '姓名', required: true },
            { name: 'phone', label: '联系电话', required: true },
            { name: 'accountUsername', label: '认定管理员账号' },
            { name: 'accountPassword', label: '初始密码（新增必填）' },
        ], statusOptions: [
            { label: '空闲', value: 'idle' },
            { label: '任务中', value: 'assigned' },
        ], pageApi: pageExamStaffInfo, createApi: createExamStaffInfo, updateApi: updateExamStaffInfo, submitApi: submitExamStaffInfo, reviewApi: reviewExamStaffInfo, disableApi: disableExamStaffInfo, enableApi: enableExamStaffInfo, deleteApi: deleteExamStaffInfo }));
}
function statusLabel(value) {
    return {
        idle: '空闲',
        assigned: '任务中',
        disabled: '停用',
    }[value] || value || '-';
}
