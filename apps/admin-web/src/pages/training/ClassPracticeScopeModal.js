import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Form, Input, Modal, Select, Space, Spin, Switch, Tree, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
const { Text } = Typography;
const { RangePicker } = DatePicker;
/**
 * exam_course.level：0 初级 … 4 高级技师（与科目标题后缀一致，不是班级 work_type_level 的 1–5）。
 */
const EXAM_COURSE_LEVEL_LABELS = {
    0: '五级/初级工',
    1: '四级/中级工',
    2: '三级/高级工',
    3: '二级/技师',
    4: '一级/高级技师',
};
/** 科目标题末尾常见简称，拼人社全称前去掉，避免「…高级三级/高级工」 */
const EXAM_COURSE_SHORT_LEVEL_SUFFIX = /(初级|中级|高级技师|高级|技师)$/;
function formatCourseOptionLabel(title, level) {
    let name = (title ?? '').trim() || '未命名科目';
    if (level == null || Number.isNaN(Number(level)))
        return name;
    const n = Number(level);
    const levelText = EXAM_COURSE_LEVEL_LABELS[n];
    if (!levelText)
        return `${name}（等级${n}）`;
    const stripped = name.replace(EXAM_COURSE_SHORT_LEVEL_SUFFIX, '').trim();
    if (stripped)
        name = stripped;
    // 例：铝电解槽操作工三级/高级工
    return `${name}${levelText}`;
}
function toKnowledgeTree(nodes) {
    return (nodes ?? []).map((n) => ({
        key: String(n.id),
        title: n.title,
        children: n.children?.length ? toKnowledgeTree(n.children) : undefined,
    }));
}
function collectKeys(nodes) {
    const out = [];
    const walk = (list) => {
        for (const n of list) {
            out.push(String(n.key));
            if (n.children?.length)
                walk(n.children);
        }
    };
    walk(nodes);
    return out;
}
function parseScopeTime(v) {
    if (!v)
        return null;
    const d = dayjs(String(v).slice(0, 10));
    return d.isValid() ? d : null;
}
export function ClassPracticeScopeModal({ open, onClose, classId, classTitle, titlePrefix = '练习范围' }) {
    const queryClient = useQueryClient();
    const [enabled, setEnabled] = useState(true);
    const [remark, setRemark] = useState('');
    const [practiceRange, setPracticeRange] = useState(null);
    const [courseIds, setCourseIds] = useState([]);
    /** knowledgeId -> courseId，切换科目时用于清理残留 */
    const [knowledgeCourseMap, setKnowledgeCourseMap] = useState({});
    const [activeCourseId, setActiveCourseId] = useState();
    const knowledgeIds = useMemo(() => Object.keys(knowledgeCourseMap).map(Number).filter((n) => !Number.isNaN(n)), [knowledgeCourseMap]);
    const scopeQuery = useQuery({
        queryKey: ['class-practice-scope', classId],
        queryFn: () => trainApi.getClassPracticeScope(classId),
        enabled: open && classId != null,
    });
    const coursesQuery = useQuery({
        queryKey: ['practice-scope-courses', open],
        queryFn: async () => {
            const all = await trainApi.listPracticeScopeCourses();
            return all.items ?? [];
        },
        enabled: open,
        staleTime: 5 * 60 * 1000,
    });
    const treeQuery = useQuery({
        queryKey: ['practice-scope-knowledge-tree', activeCourseId],
        queryFn: () => trainApi.listPracticeScopeKnowledgeTree(activeCourseId),
        enabled: open && activeCourseId != null,
    });
    useEffect(() => {
        if (!open || !scopeQuery.data)
            return;
        const d = scopeQuery.data;
        setEnabled(d.enabled !== 0);
        setRemark(d.remark ?? '');
        const start = parseScopeTime(d.practiceStartTime);
        const end = parseScopeTime(d.practiceEndTime);
        if (start && end) {
            setPracticeRange([start, end]);
        }
        else if (start || end) {
            const one = start ?? end;
            setPracticeRange([one, one]);
        }
        else {
            setPracticeRange(null);
        }
        setCourseIds(d.courseIds ?? []);
        const map = {};
        for (const k of d.knowledges ?? []) {
            if (k.knowledgeId != null && k.courseId != null) {
                map[k.knowledgeId] = k.courseId;
            }
        }
        setKnowledgeCourseMap(map);
        setActiveCourseId(d.courseIds?.[0]);
    }, [open, scopeQuery.data]);
    useEffect(() => {
        if (!open)
            return;
        if (courseIds.length === 0) {
            setActiveCourseId(undefined);
            return;
        }
        if (activeCourseId == null || !courseIds.includes(activeCourseId)) {
            setActiveCourseId(courseIds[0]);
        }
    }, [open, courseIds, activeCourseId]);
    const courseOptions = useMemo(() => {
        return (coursesQuery.data ?? []).map((c) => ({
            value: c.id,
            label: formatCourseOptionLabel(c.title, c.level),
        }));
    }, [coursesQuery.data]);
    const treeData = useMemo(() => toKnowledgeTree((treeQuery.data ?? [])), [treeQuery.data]);
    const checkedKeysForActive = useMemo(() => {
        if (activeCourseId == null)
            return [];
        return Object.entries(knowledgeCourseMap)
            .filter(([, cid]) => cid === activeCourseId)
            .map(([kid]) => String(kid));
    }, [knowledgeCourseMap, activeCourseId]);
    const saveMu = useMutation({
        mutationFn: () => trainApi.saveClassPracticeScope(classId, {
            enabled: enabled ? 1 : 0,
            remark: remark.trim() || undefined,
            practiceStartTime: practiceRange?.[0]?.format('YYYY-MM-DD') ?? null,
            practiceEndTime: practiceRange?.[1]?.format('YYYY-MM-DD') ?? null,
            courseIds,
            knowledgeIds,
        }),
        onSuccess: (res) => {
            const auto = res?.reviewSyncAuto;
            if (auto?.ok === false) {
                message.warning(`试题配置已保存，但同步学未自动开通：${auto.message || '请到「同步学」Tab 手动配置'}`);
            }
            else if (auto?.message) {
                message.success(`试题配置已保存。${auto.message}`);
            }
            else {
                message.success('试题配置已保存');
            }
            queryClient.invalidateQueries({ queryKey: ['class-practice-scope', classId] });
            queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
            onClose();
        },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const suggestMu = useMutation({
        mutationFn: () => trainApi.suggestClassPracticeCourses(classId),
        onSuccess: (res) => {
            const ids = (res.courses ?? []).map((c) => c.courseId).filter(Boolean);
            if (!ids.length) {
                message.warning('未找到与班级工种匹配的科目，请手动选择');
                return;
            }
            setCourseIds(ids);
            setKnowledgeCourseMap({});
            setActiveCourseId(ids[0]);
            message.success(`已填充 ${ids.length} 个推荐科目`);
        },
        onError: (e) => message.error(e.message || '推荐失败'),
    });
    const onCourseChange = (ids) => {
        const next = new Set(ids);
        setCourseIds(ids);
        setKnowledgeCourseMap((prev) => {
            const kept = {};
            for (const [kid, cid] of Object.entries(prev)) {
                if (next.has(cid))
                    kept[Number(kid)] = cid;
            }
            return kept;
        });
    };
    const onCheckKnowledge = (checked) => {
        if (activeCourseId == null)
            return;
        const keys = Array.isArray(checked) ? checked : checked.checked;
        const activeKeys = new Set(keys.map(String));
        const treeKeys = new Set(collectKeys(treeData));
        setKnowledgeCourseMap((prev) => {
            const next = {};
            for (const [kid, cid] of Object.entries(prev)) {
                const id = Number(kid);
                if (!treeKeys.has(String(id))) {
                    next[id] = cid;
                }
            }
            for (const key of activeKeys) {
                const id = Number(key);
                if (!Number.isNaN(id))
                    next[id] = activeCourseId;
            }
            return next;
        });
    };
    return (_jsxs(Modal, { title: classTitle ? `${titlePrefix}：${classTitle}` : `班级${titlePrefix}`, open: open, width: 720, onCancel: onClose, destroyOnHidden: true, footer: [
            _jsx(Button, { onClick: onClose, children: "\u53D6\u6D88" }, "cancel"),
            _jsx(Button, { type: "primary", loading: saveMu.isPending, onClick: () => saveMu.mutate(), children: "\u4FDD\u5B58" }, "save"),
        ], children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u914D\u7F6E\u672C\u73ED\u53EF\u5237\u79D1\u76EE\u3001\u77E5\u8BC6\u70B9\u4E0E\u5237\u9898\u65E5\u671F\u3002\u4FDD\u5B58\u540E\u5C06\u81EA\u52A8\u7ED1\u5B9A\u540C\u6B65\u5B66\u8003\u8BD5\u8BA1\u5212\u3001\u5199\u5165\u590D\u4E60\u914D\u7F6E\u5E76\u5F00\u901A\u540D\u5355/\u7070\u5EA6\uFF1B\u5B66\u5458\u53EF\u5728 App\u300C\u590D\u4E60\u4EFB\u52A1\u300D\u4E2D\u8FDB\u5165\u540C\u6B65\u7EC3\u4E60\u3002\u672A\u8BBE\u7F6E\u65E5\u671F\u5219\u4E0D\u8FC7\u671F\uFF1B\u8FC7\u4E86\u622A\u6B62\u65E5\u5B66\u5458\u65E0\u6CD5\u4ECE\u73ED\u7EA7\u5165\u53E3\u5237\u9898\u3002" }), scopeQuery.isLoading ? (_jsx("div", { style: { textAlign: 'center', padding: 32 }, children: _jsx(Spin, {}) })) : (_jsxs(Form, { layout: "vertical", children: [_jsxs(Form.Item, { label: "\u542F\u7528\u7EC3\u4E60\u8303\u56F4", children: [_jsx(Switch, { checked: enabled, onChange: setEnabled }), _jsx(Text, { type: "secondary", style: { marginLeft: 12 }, children: "\u505C\u7528\u540E\u7981\u6B62\u6309\u73ED\u5237\u9898\uFF08\u5168\u5C40\u5237\u9898\u5165\u53E3\u4ECD\u53EF\u7528\uFF09" })] }), _jsx(Form.Item, { label: "\u5237\u9898\u65E5\u671F", extra: "\u6309\u65E5\u8BBE\u7F6E\uFF0C\u53EF\u4E0D\u586B\u3002\u6E05\u7A7A\u8868\u793A\u4E0D\u8FC7\u671F\uFF1B\u622A\u6B62\u65E5\u671F\u5F53\u5929\u4ECD\u53EF\u5237\u9898\u3002", children: _jsx(RangePicker, { allowClear: true, style: { width: '100%' }, value: practiceRange, onChange: (vals) => {
                                if (vals?.[0] && vals?.[1]) {
                                    setPracticeRange([vals[0], vals[1]]);
                                }
                                else {
                                    setPracticeRange(null);
                                }
                            }, placeholder: ['开始日期（可选）', '截止日期（可选）'] }) }), _jsx(Form.Item, { label: _jsxs(Space, { children: [_jsx("span", { children: "\u79D1\u76EE\u767D\u540D\u5355" }), _jsx(Button, { size: "small", loading: suggestMu.isPending, onClick: () => suggestMu.mutate(), children: "\u6309\u73ED\u7EA7\u5DE5\u79CD\u63A8\u8350" })] }), children: _jsx(Select, { mode: "multiple", allowClear: true, placeholder: "\u9009\u62E9\u53EF\u5237\u79D1\u76EE", options: courseOptions, value: courseIds, onChange: onCourseChange, optionFilterProp: "label", loading: coursesQuery.isLoading, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u77E5\u8BC6\u70B9\u767D\u540D\u5355\uFF08\u53EF\u9009\uFF09", extra: "\u4E0D\u9009\u77E5\u8BC6\u70B9\u8868\u793A\u6240\u9009\u79D1\u76EE\u4E0B\u4E0D\u9650\u77E5\u8BC6\u70B9\uFF1B\u6309\u79D1\u76EE\u5207\u6362\u6811\u52FE\u9009\u3002\u53D6\u6D88\u79D1\u76EE\u4F1A\u81EA\u52A8\u6E05\u6389\u8BE5\u79D1\u77E5\u8BC6\u70B9\u3002", children: courseIds.length === 0 ? (_jsx(Text, { type: "secondary", children: "\u8BF7\u5148\u9009\u62E9\u79D1\u76EE" })) : (_jsxs(_Fragment, { children: [_jsx(Select, { style: { width: '100%', marginBottom: 8 }, options: courseIds.map((id) => {
                                        const opt = courseOptions.find((o) => o.value === id);
                                        return { value: id, label: opt?.label ?? `#${id}` };
                                    }), value: activeCourseId, onChange: (v) => setActiveCourseId(v) }), treeQuery.isLoading ? (_jsx(Spin, {})) : treeData.length === 0 ? (_jsx(Text, { type: "secondary", children: "\u8BE5\u79D1\u76EE\u6682\u65E0\u77E5\u8BC6\u70B9\u6811" })) : (_jsx("div", { style: { maxHeight: 280, overflow: 'auto', border: '1px solid #f0f0f0', padding: 8 }, children: _jsx(Tree, { checkable: true, defaultExpandAll: true, treeData: treeData, checkedKeys: checkedKeysForActive, onCheck: onCheckKnowledge }) })), _jsxs(Text, { type: "secondary", style: { display: 'block', marginTop: 8 }, children: ["\u5DF2\u9009\u77E5\u8BC6\u70B9\u5408\u8BA1 ", knowledgeIds.length, " \u4E2A"] })] })) }), _jsx(Form.Item, { label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2, value: remark, onChange: (e) => setRemark(e.target.value), placeholder: "\u53EF\u9009", maxLength: 500 }) })] }))] }));
}
