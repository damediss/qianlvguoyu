import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, DatePicker, Descriptions, Form, Input, Modal, Select, Space, Spin, Switch, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
const { Text } = Typography;
const { RangePicker } = DatePicker;
function parseDay(v) {
    if (!v)
        return null;
    const d = dayjs(String(v).slice(0, 10));
    return d.isValid() ? d : null;
}
function navigateTo(path) {
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
}
export function ClassReviewSyncModal({ open, onClose, classId, classTitle }) {
    const queryClient = useQueryClient();
    const [examId, setExamId] = useState();
    const [enabled, setEnabled] = useState(true);
    const [remark, setRemark] = useState('');
    const [syncRange, setSyncRange] = useState(null);
    /** 仅在打开弹窗时灌入表单，避免 ensure/refetch 冲掉未保存编辑 */
    const seededForOpenRef = useRef(false);
    const configQuery = useQuery({
        queryKey: ['class-review-sync-config', classId],
        queryFn: () => trainApi.getClassReviewSyncConfig(classId),
        enabled: open && classId != null,
    });
    const optionsQuery = useQuery({
        queryKey: ['class-review-sync-exam-options', classId],
        queryFn: () => trainApi.listClassReviewSyncExamOptions(classId),
        enabled: open && classId != null,
        staleTime: 60_000,
    });
    useEffect(() => {
        if (!open) {
            seededForOpenRef.current = false;
            return;
        }
        if (!configQuery.data || seededForOpenRef.current)
            return;
        seededForOpenRef.current = true;
        const d = configQuery.data;
        setExamId(d.examId ?? undefined);
        setEnabled(!!d.enabled);
        setRemark(d.remark ?? '');
        const start = parseDay(d.syncStartTime);
        const end = parseDay(d.syncEndTime);
        if (start && end)
            setSyncRange([start, end]);
        else if (start || end) {
            const one = start ?? end;
            setSyncRange([one, one]);
        }
        else
            setSyncRange(null);
    }, [open, configQuery.data]);
    const examOptions = useMemo(() => {
        const items = optionsQuery.data?.items ?? [];
        return items.map((it) => ({
            value: it.examId,
            label: (_jsxs(Space, { size: 6, children: [_jsx("span", { children: it.title }), it.matched ? _jsx(Tag, { color: "success", children: "\u5DE5\u79CD\u5339\u914D" }) : it.workTypeMatched ? _jsx(Tag, { children: "\u5DE5\u79CD\u76F8\u8FD1" }) : null] })),
            search: it.title,
        }));
    }, [optionsQuery.data]);
    const saveMu = useMutation({
        mutationFn: () => {
            if (!examId)
                throw new Error('请选择同步学考试计划');
            return trainApi.saveClassReviewSyncConfig(classId, {
                examId,
                enabled: enabled ? 1 : 0,
                syncStartTime: syncRange?.[0]?.format('YYYY-MM-DD') ?? null,
                syncEndTime: syncRange?.[1]?.format('YYYY-MM-DD') ?? null,
                remark: remark.trim() || null,
            });
        },
        onSuccess: (cfg) => {
            const inserted = cfg.enroll?.inserted ?? 0;
            const removedStale = cfg.enroll?.removedStale ?? 0;
            const reviewError = cfg.reviewError || cfg.enroll?.reviewError;
            if (cfg.enrollError) {
                message.warning(`绑定已保存，但名单同步失败：${cfg.enrollError}`);
            }
            else if (reviewError) {
                message.warning(`绑定已保存，但复习任务未开通：${reviewError}`);
            }
            else if (cfg.grayError) {
                message.warning(`绑定已保存，但灰度开通失败：${cfg.grayError}`);
            }
            else if (inserted > 0 || removedStale > 0) {
                const parts = [`同步学配置已保存`];
                if (inserted > 0)
                    parts.push(`写入 ${inserted} 名学员`);
                if (removedStale > 0)
                    parts.push(`已移除 ${removedStale} 名非本班考生`);
                message.success(`${parts.join('，')}；请学员在「复习任务」中进入同步练习`);
            }
            else {
                message.success('同步学配置已保存；请学员在「提升 → 复习任务」中进入同步练习');
            }
            void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
        },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const enrollMu = useMutation({
        mutationFn: () => trainApi.enrollClassReviewSyncRoster(classId),
        onSuccess: (r) => {
            const staleHint = (r.removedStale ?? 0) > 0 ? `，移除非本班 ${r.removedStale}` : '';
            message.success(`名单同步完成：新增 ${r.inserted}，已在册 ${r.existed}，无账号跳过 ${r.skippedNoAccount}${staleHint}`);
            void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
        },
        onError: (e) => message.error(e.message || '同步名单失败'),
    });
    const grayMu = useMutation({
        mutationFn: () => trainApi.ensureClassReviewSyncGray(classId),
        onSuccess: (r) => {
            message.success(r.message || '已处理灰度');
            void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
        },
        onError: (e) => message.error(e.message || '加入灰度失败'),
    });
    const cfg = configQuery.data;
    const roster = cfg?.roster;
    const canOpenActions = !!(cfg?.configured && cfg.examId);
    return (_jsxs(Modal, { title: `同步学配置${classTitle ? ` · ${classTitle}` : ''}`, open: open, onCancel: onClose, width: 720, destroyOnClose: true, footer: [
            _jsx(Button, { onClick: onClose, children: "\u5173\u95ED" }, "cancel"),
            _jsx(Button, { type: "primary", loading: saveMu.isPending, disabled: !examId, onClick: () => saveMu.mutate(), children: "\u4FDD\u5B58\u7ED1\u5B9A" }, "save"),
        ], children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u901A\u9053 C \u00B7 AI \u540C\u6B65\u5B66", description: "\u4E3A\u672C\u73ED\u7ED1\u5B9A\u4E00\u5957\u540C\u6B65\u5B66\u8003\u8BD5\u8BA1\u5212\u3002\u4FDD\u5B58\u65F6\u4F1A\u81EA\u52A8\u628A\u672C\u73ED\u6709\u8D26\u53F7\u5B66\u5458\u5199\u5165\u8003\u751F\u540D\u5355\uFF0C\u5E76\u6309\u9700\u52A0\u5165\u7070\u5EA6\u3002" }), _jsxs(Spin, { spinning: configQuery.isLoading || optionsQuery.isLoading, children: [_jsxs(Form, { layout: "vertical", children: [_jsx(Form.Item, { label: "\u540C\u6B65\u5B66\u8003\u8BD5\u8BA1\u5212", required: true, children: _jsx(Select, { showSearch: true, allowClear: true, placeholder: optionsQuery.data?.items?.length ? '请选择考试计划' : '本培训下暂无考试计划', value: examId, options: examOptions, optionFilterProp: "search", filterOption: (input, option) => String(option?.search ?? '').toLowerCase().includes(input.toLowerCase()), onChange: (v) => setExamId(v) }) }), _jsxs(Form.Item, { label: "\u542F\u7528", children: [_jsx(Switch, { checked: enabled, onChange: setEnabled, checkedChildren: "\u542F\u7528", unCheckedChildren: "\u505C\u7528" }), _jsx(Text, { type: "secondary", style: { marginLeft: 12 }, children: "\u505C\u7528\u540E\u5B66\u5458\u73ED\u7EA7\u5165\u53E3\u5C06\u9690\u85CF" })] }), _jsxs(Form.Item, { label: "\u5F00\u7EC3\u65F6\u95F4\u7A97\uFF08\u53EF\u9009\uFF09", children: [_jsx(RangePicker, { value: syncRange, onChange: (v) => setSyncRange(v), style: { width: '100%' } }), _jsx(Text, { type: "secondary", style: { display: 'block', marginTop: 4 }, children: "\u7559\u7A7A\u8868\u793A\u4E0D\u9650\uFF1B\u7A97\u53E3\u5916\u5B66\u5458\u5165\u53E3\u9690\u85CF" })] }), _jsx(Form.Item, { label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { value: remark, onChange: (e) => setRemark(e.target.value), rows: 2, maxLength: 255, showCount: true, placeholder: "\u53EF\u9009" }) })] }), cfg && (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { size: "small", bordered: true, column: 2, title: "\u5F00\u901A\u6458\u8981", style: { marginBottom: 12 }, children: [_jsx(Descriptions.Item, { label: "\u7070\u5EA6\u603B\u5F00\u5173", children: cfg.grayEnabled ? _jsx(Tag, { color: "warning", children: "\u7070\u5EA6\u4E2D" }) : _jsx(Tag, { color: "success", children: "\u5168\u91CF\u5F00\u653E" }) }), _jsx(Descriptions.Item, { label: "\u672C\u8BA1\u5212\u767D\u540D\u5355", children: cfg.examInGray ? _jsx(Tag, { color: "success", children: "\u5DF2\u653E\u884C" }) : _jsx(Tag, { color: "error", children: "\u672A\u52A0\u5165" }) }), _jsx(Descriptions.Item, { label: "\u73ED\u7EA7\u5B66\u5458", children: roster?.total ?? 0 }), _jsx(Descriptions.Item, { label: "\u6709\u8D26\u53F7", children: roster?.withPlatformUser ?? 0 }), _jsx(Descriptions.Item, { label: "\u5DF2\u5728\u8003\u751F\u540D\u5355", children: roster?.inExamPeople ?? 0 }), _jsx(Descriptions.Item, { label: "\u5F85\u540C\u6B65\u540D\u5355", children: _jsx(Text, { type: (roster?.missingEnroll ?? 0) > 0 ? 'warning' : undefined, children: roster?.missingEnroll ?? 0 }) }), _jsxs(Descriptions.Item, { label: "\u65E0\u8D26\u53F7", span: 2, children: [roster?.missingAccount ?? 0, (roster?.missingAccount ?? 0) > 0 && (_jsx(Text, { type: "secondary", children: "\uFF08\u9700\u5B66\u5458\u5148\u767B\u5F55 App \u7ED1\u5B9A\u8D26\u53F7\uFF09" }))] })] }), (roster?.missingEnroll ?? 0) > 0 && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: `还有 ${roster?.missingEnroll} 名有账号学员未写入考生名单，学员将无法生成同步学计划` })), _jsxs(Space, { wrap: true, style: { marginBottom: 8 }, children: [_jsx(Button, { disabled: !canOpenActions, loading: enrollMu.isPending, onClick: () => {
                                            Modal.confirm({
                                                title: '同步本班名单',
                                                content: '将把本班有账号学员写入所选考试计划的考生名单（幂等，不自动建复习计划）。确定继续？',
                                                onOk: () => enrollMu.mutateAsync(),
                                            });
                                        }, children: "\u540C\u6B65\u672C\u73ED\u540D\u5355" }), _jsx(Button, { disabled: !canOpenActions || !cfg.grayEnabled || cfg.examInGray, loading: grayMu.isPending, onClick: () => {
                                            Modal.confirm({
                                                title: '加入灰度',
                                                content: '将把该考试计划写入同步学灰度白名单（计划级，可能影响同计划其他班级）。确定继续？',
                                                onOk: () => grayMu.mutateAsync(),
                                            });
                                        }, children: "\u52A0\u5165\u7070\u5EA6" }), cfg.statsPath && (_jsx(Button, { type: "link", onClick: () => navigateTo(cfg.statsPath), children: "\u67E5\u770B\u540C\u6B65\u5B66\u5B66\u60C5" }))] })] }))] })] }));
}
