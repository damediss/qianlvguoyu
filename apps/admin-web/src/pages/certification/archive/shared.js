import { jsx as _jsx } from "react/jsx-runtime";
import { Tag } from 'antd';
export const taskMaterialOptions = [
    { label: '职业技能等级认定评价结果确认表', value: 'RESULT_CONFIRM_FORM' },
    { label: '广西壮族自治区职业技能鉴定业务申请表', value: 'BUSINESS_APPLICATION' },
    { label: '四类人员安排表', value: 'STAFF_ARRANGEMENT' },
    { label: '职业技能等级认定成绩公示文件', value: 'SCORE_PUBLICITY' },
    { label: '广西技能人才评价质量督导表', value: 'QUALITY_SUPERVISION' },
    { label: '职业技能等级认定成绩花名册', value: 'SCORE_ROSTER' },
    { label: '职业技能等级获证人员花名册', value: 'CERTIFIED_ROSTER' },
    { label: '职业技能等级认定理论考务材料', value: 'THEORY_EXAM_MATERIAL' },
    { label: '职业技能等级认定技能考务材料', value: 'SKILL_EXAM_MATERIAL' },
];
// 任务状态（status）的 4 个取值
const taskStatusLabelMap = {
    in_progress: '进行中',
    pending_review: '待审核',
    rejected: '已驳回',
    completed: '已完成',
};
const taskStatusColorMap = {
    in_progress: 'processing',
    pending_review: 'warning',
    rejected: 'error',
    completed: 'success',
};
export function taskStageStatusTag(status) {
    const label = status ? taskStatusLabelMap[status] || status : '-';
    const color = status ? taskStatusColorMap[status] || 'default' : 'default';
    return _jsx(Tag, { color: color, children: label });
}
// 任务阶段（current_phase）的 4 个取值
const taskPhaseLabelMap = {
    task_creation: '任务创建阶段',
    preparation: '认定筹备阶段',
    archive: '认定归档阶段',
    certification_completed: '认定完成',
};
const taskPhaseColorMap = {
    task_creation: 'processing',
    preparation: 'gold',
    archive: 'purple',
    certification_completed: 'success',
};
export function taskPhaseTag(phase) {
    const normalized = phase === 'project_creation' ? 'task_creation' : phase;
    const label = normalized ? taskPhaseLabelMap[normalized] || normalized : '-';
    const color = normalized ? taskPhaseColorMap[normalized] || 'default' : 'default';
    return _jsx(Tag, { color: color, children: label });
}
export function materialArchiveTag(materialArchived, candidateMaterialArchived, taskMaterialArchived) {
    if (materialArchived || candidateMaterialArchived) {
        return _jsx(Tag, { color: "success", children: "\u5DF2\u5F52\u6863" });
    }
    if (taskMaterialArchived) {
        return _jsx(Tag, { color: "processing", children: "\u8BA4\u5B9A\u6750\u6599\u5DF2\u9F50" });
    }
    return _jsx(Tag, { children: "\u672A\u5F52\u6863" });
}
export function archiveStatusTag(value) {
    const map = {
        NOT_ARCHIVED: { text: '未归档', color: 'default' },
        BASIC_ARCHIVED: { text: '基础信息已归档', color: 'processing' },
        MATERIAL_ARCHIVED: { text: '资料已归档', color: 'warning' },
        ARCHIVED: { text: '认定归档已完成', color: 'success' },
    };
    const item = value ? map[value] : undefined;
    return _jsx(Tag, { color: item?.color || 'default', children: item?.text || value || '-' });
}
export function batchStatusTag(value) {
    const map = {
        UPLOADED: { text: '已上传', color: 'default' },
        PARSING: { text: '解析中', color: 'processing' },
        PARSE_SUCCESS: { text: '解析成功', color: 'success' },
        PARSE_FAILED: { text: '解析失败', color: 'error' },
        CONFIRMED: { text: '已确认入库', color: 'success' },
        CANCELLED: { text: '已取消', color: 'default' },
    };
    const item = value ? map[value] : undefined;
    return _jsx(Tag, { color: item?.color || 'default', children: item?.text || value || '-' });
}
export function dateTimeText(value) {
    return value ? value.replace('T', ' ') : '-';
}
export function fileSizeText(value) {
    if (!value) {
        return '-';
    }
    if (value >= 1024 * 1024) {
        return `${(value / 1024 / 1024).toFixed(2)} MB`;
    }
    if (value >= 1024) {
        return `${(value / 1024).toFixed(1)} KB`;
    }
    return `${value} B`;
}
function parseSnapshotJson(raw) {
    if (!raw) {
        return [];
    }
    try {
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    }
    catch {
        return [];
    }
}
function uniqueTexts(values) {
    return [...new Set(values.filter(Boolean))];
}
export function formatScopeSnapshot(raw) {
    const levels = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.levelName || item.levelCode || '').trim()));
    return levels.length ? levels.join('、') : '-';
}
export function formatExamStaffSnapshot(raw) {
    const lines = parseSnapshotJson(raw)
        .map((item) => {
        const name = String(item.staffName || '').trim();
        const role = String(item.roleName || item.taskRole || '').trim();
        if (!name) {
            return '';
        }
        return role ? `${name}（${role}）` : name;
    })
        .filter(Boolean);
    return lines.length ? lines.join('\n') : '-';
}
export function formatDeviceSnapshot(raw) {
    const names = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.equipmentName || '').trim()));
    return names.length ? names.join('、') : '-';
}
export function formatRoomSnapshot(raw) {
    const names = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.siteName || '').trim()));
    return names.length ? names.join('、') : '-';
}
export function formatEvaluatorSnapshot(raw) {
    const names = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.assessorName || '').trim()));
    return names.length ? names.join('、') : '-';
}
