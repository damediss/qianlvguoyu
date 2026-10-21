import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Tag } from 'antd';
import dayjs from 'dayjs';
/** 认定筹备已完成：比认定准备略深的绿色，背景更透明 */
export const preparationCompletedTagStyle = {
    color: '#389e0d',
    background: 'rgba(56, 158, 13, 0.08)',
    borderColor: 'rgba(56, 158, 13, 0.2)',
};
export const activeStatusOptions = [
    { label: '启用', value: 'active' },
    { label: '停用', value: 'inactive' },
];
export const commonStatusOptions = [
    { label: '草稿', value: 'draft' },
    { label: '进行中', value: 'running' },
    { label: '已完成', value: 'completed' },
    { label: '已关闭', value: 'closed' },
];
export const levelOptions = [
    { label: '初级', value: 'junior' },
    { label: '中级', value: 'intermediate' },
    { label: '高级', value: 'senior' },
    { label: '技师', value: 'technician' },
    { label: '高级技师', value: 'senior_technician' },
];
export const documentTemplates = [
    { code: 'DOC001', name: '报名信息表', required: true, mode: 'generate' },
    { code: 'DOC002', name: '两寸证件照', required: true, mode: 'upload' },
];
export const importSourceLabelMap = {
    staging: '测试学员',
    training: '培训学员',
    other: '其他学员',
    excel: '其他学员',
    manual: '手工录入',
};
export function importSourceLabel(source) {
    if (!source) {
        return '-';
    }
    return importSourceLabelMap[source] || source;
}
export function dash(value) {
    return value === undefined || value === null || value === '' ? '-' : String(value);
}
export function dateText(value) {
    return value ? dayjs(value).format('YYYY-MM-DD') : '-';
}
export function dateTimeText(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}
export function formatDate(value) {
    if (!value) {
        return undefined;
    }
    if (dayjs.isDayjs(value)) {
        return value.format('YYYY-MM-DD');
    }
    return String(value);
}
export function statusTag(status) {
    const labelMap = {
        active: '启用',
        inactive: '停用',
        draft: '草稿',
        running: '进行中',
        completed: '已完成',
        configured: '已配置',
        generated: '已生成',
        failed: '失败',
        uploaded: '已上传',
        approved: '已通过',
        rejected: '已驳回',
        pending: '待处理',
        dispatched: '已发放',
        scoring: '填写中',
        submitted: '已提交',
        reviewed: '已复核',
        returned: '需整改',
        assigned: '已发放',
        in_progress: '填写中',
        rework: '需整改',
        preparing: '筹备中',
        idle: '空闲',
        project_prepared: '认定准备已完成',
        preparation_completed: '认定筹备已完成',
        archive_completed: '认定归档已完成',
        missing: '未导入',
        pending_review: '待审核',
    };
    if (status === 'preparation_completed') {
        return _jsx(Tag, { style: preparationCompletedTagStyle, children: labelMap[status] });
    }
    const color = status === 'approved'
        || status === 'completed'
        || status === 'generated'
        || status === 'reviewed'
        || status === 'project_prepared'
        || status === 'archive_completed'
        || status === 'idle'
        ? 'success'
        : status === 'rejected' || status === 'failed' || status === 'returned'
            ? 'error'
            : status === 'active' || status === 'running' || status === 'dispatched' || status === 'scoring' || status === 'submitted'
                ? 'processing'
                : status === 'inactive'
                    ? 'default'
                    : 'warning';
    return _jsx(Tag, { color: color, children: status ? labelMap[status] || status : '-' });
}
export function boolTag(value) {
    return _jsx(Tag, { color: value ? 'success' : 'default', children: value ? '是' : '否' });
}
/** 考生资料上传进度（已上传/应上传），与考生资料页统计口径一致 */
export function candidateDocumentRatio(record) {
    const required = record.documentRequiredCount ?? documentTemplates.length;
    const total = Math.max(record.documentTotalCount ?? 0, required) || documentTemplates.length;
    const uploaded = record.documentUploadedCount ?? 0;
    return { uploaded, total: Math.max(total, uploaded) };
}
export function candidateDocumentRatioTag(record) {
    const { uploaded, total } = candidateDocumentRatio(record);
    const uploadComplete = total > 0 && uploaded >= total;
    return (_jsxs(Tag, { color: uploadComplete ? 'success' : uploaded > 0 ? 'processing' : 'default', children: [uploaded, "/", total] }));
}
export const formGridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
    gap: 12,
};
