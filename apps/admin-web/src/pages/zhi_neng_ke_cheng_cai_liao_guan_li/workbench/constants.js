/**
 * 生成工作台共享常量与格式化工具（原 TheoryQuestionGenPage 内联，重构后抽出复用）。
 */
import dayjs from 'dayjs';
export const LEVEL_NAME = {
    1: '高级技师', 2: '技师', 3: '高级工', 4: '中级工', 5: '初级工',
};
/** 指引下限推荐题量（每考评点）：五/四级 单选4+判断2；三/二/一级 单选3+多选2+判断2；
 * 二/一级另有主观题轮转配额：sub5/sub10 = 每 N 个考评点 1 道 5 分制/10 分制主观题（120 点 → 各 40 道） */
export const QUOTA_DEFAULTS = {
    5: { single: 4, judge: 2 },
    4: { single: 4, judge: 2 },
    3: { single: 3, judge: 2, multi: 2 },
    2: { single: 3, judge: 2, multi: 2, sub5: 3, sub10: 3 },
    1: { single: 3, judge: 2, multi: 2, sub5: 3, sub10: 3 },
};
export const TYPE_LABEL = {
    single: '单选题', multi: '多选题', judge: '判断题', essay: '简答题',
    cal: '计算题', draw: '绘图题', discuss: '论述题', case_analysis: '案例分析题',
};
/** 指引难度档位：1易 2较易 3中 4较难 5难 */
export const DIFFICULTY_LABEL = {
    1: '易', 2: '较易', 3: '中', 4: '较难', 5: '难',
};
export const STATUS_TAG = {
    pending: { color: 'default', text: '等待中' },
    running: { color: 'processing', text: '生成中' },
    paused: { color: 'warning', text: '已暂停' },
    done: { color: 'success', text: '已完成' },
    failed: { color: 'error', text: '失败' },
};
/** 细目表批次下拉选项 label：导入时间前置（同工种同等级重复导入时标题完全相同，时间是最可靠区分） */
export const bpOptionLabel = (b) => {
    const time = b.createdAt ? dayjs(b.createdAt).format('MM-DD HH:mm') : '时间未知';
    return `${time} · ${b.title}（${b.pointCount}点）`;
};
/** 批次展示 label：开始时间 + 批次短号 + 中文状态 */
export const genBatchOptionLabel = (b) => {
    const time = b.startedAt ?? b.createdAt ?? b.finishedAt;
    const timeText = time ? dayjs(time).format('MM-DD HH:mm') : '时间未知';
    const statusText = STATUS_TAG[b.status]?.text ?? b.status;
    return `${timeText} · 批次${b.id.slice(-8)}（${statusText}）`;
};
/** 草稿 options 为 JSON 字符串（[{"label":"A","content":"..."}]），零信任解析：失败返回空 */
export const parseDraftOptions = (raw) => {
    if (!raw)
        return [];
    try {
        const arr = JSON.parse(raw);
        return Array.isArray(arr)
            ? arr.filter((o) => o && o.label && o.content)
            : [];
    }
    catch {
        return [];
    }
};
/** 导入校验报告（check_report JSON）零信任解析：返回 errors 列表（权重合计≠100%、X/Y/Z 占比超标等） */
export const parseCheckReport = (raw) => {
    if (!raw)
        return [];
    try {
        const obj = JSON.parse(raw);
        return Array.isArray(obj?.errors)
            ? obj.errors.filter((e) => typeof e === 'string' && e)
            : [];
    }
    catch {
        return [];
    }
};
