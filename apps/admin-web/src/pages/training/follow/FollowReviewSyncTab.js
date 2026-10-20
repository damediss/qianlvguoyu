import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Descriptions, Space, Tag, Typography, message } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import { ClassReviewSyncModal } from '../ClassReviewSyncModal';
import { StatsOverviewPage } from '../../exam/reviewSyncStats/StatsOverviewPage';
import { useAuthStore } from '../../../stores/authStore';
/**
 * 跟班「同步学」Tab：通道 C 绑定 exam + 开通名单/灰度。
 * Tab 激活时自动把本班学员写入考生名单（未绑定则匹配/新建计划）。
 * 学情直接嵌在本页下方，无需跳转。
 */
export function FollowReviewSyncTab({ classId, classTitle, active }) {
    const queryClient = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    const canViewStats = hasPerm('exam:review-sync:stats:view');
    const [modalOpen, setModalOpen] = useState(false);
    const [ensureState, setEnsureState] = useState('idle');
    const [ensureMessage, setEnsureMessage] = useState(null);
    const ensureInflightRef = useRef(new Map());
    const configQuery = useQuery({
        queryKey: ['class-review-sync-config', classId],
        queryFn: () => trainApi.getClassReviewSyncConfig(classId),
        enabled: active && classId != null,
    });
    useEffect(() => {
        if (!active || classId == null)
            return;
        let cancelled = false;
        const key = String(classId);
        setEnsureState('loading');
        setEnsureMessage(null);
        let inflight = ensureInflightRef.current.get(key);
        if (!inflight) {
            inflight = trainApi
                .ensureClassReviewSyncRoster(classId)
                .finally(() => {
                ensureInflightRef.current.delete(key);
            });
            ensureInflightRef.current.set(key, inflight);
        }
        void inflight
            .then((r) => {
            if (cancelled)
                return;
            if (r.config?.configured) {
                queryClient.setQueryData(['class-review-sync-config', classId], r.config);
            }
            void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
            const inserted = r.enroll?.inserted ?? 0;
            const removedStale = r.enroll?.removedStale ?? 0;
            if (r.ok === false) {
                setEnsureState('error');
                const tip = r.action === 'forbidden'
                    ? r.message || '当前账号无法自动开通同步学，请联系教务配置'
                    : r.action === 'mismatch' || r.action === 'exam_gone'
                        ? r.message || '当前绑定异常，请打开同步学配置手动处理'
                        : r.message || '自动同步学员失败，请打开同步学配置处理';
                setEnsureMessage(tip);
                message.warning(tip);
                return;
            }
            setEnsureState('done');
            setEnsureMessage(r.message || null);
            if (r.action === 'created' || inserted > 0 || removedStale > 0) {
                const fallback = removedStale > 0
                    ? `已同步本班学员（新增 ${inserted}，移除非本班 ${removedStale}）`
                    : `已将本班学员同步到同步学（新增 ${inserted}）`;
                message.success(r.message || fallback);
            }
        })
            .catch((e) => {
            if (cancelled)
                return;
            setEnsureState('error');
            setEnsureMessage(e.message || '自动同步学员失败');
            message.warning(e.message || '自动同步学员失败');
        });
        return () => {
            cancelled = true;
        };
    }, [active, classId, queryClient]);
    const cfg = configQuery.data;
    const roster = cfg?.roster;
    const resolve = cfg?.resolve;
    const reasonLabel = resolve?.reasonCode === 'DISABLED'
        ? '已停用'
        : resolve?.reasonCode === 'WINDOW'
            ? '不在开练窗口'
            : resolve?.reasonCode === 'EXAM_GONE'
                ? '考试计划已失效'
                : resolve?.reasonCode === 'NOT_CONFIGURED'
                    ? '未配置'
                    : null;
    const examIdNum = useMemo(() => {
        const raw = cfg?.examId;
        if (raw == null || raw === '')
            return 0;
        const n = Number(raw);
        return Number.isFinite(n) && n > 0 ? n : 0;
    }, [cfg?.examId]);
    const showStats = active && !!cfg?.configured && !cfg?.examGone && examIdNum > 0;
    const stillProvisioning = !cfg?.configured && (ensureState === 'idle' || ensureState === 'loading' || configQuery.isLoading);
    const provisionFailed = !cfg?.configured && ensureState === 'error';
    const provisionEmpty = !cfg?.configured && ensureState === 'done' && !configQuery.isFetching;
    /** 已配置但错绑/计划失效：仍展示配置摘要，并置顶告警引导手动改绑 */
    const bindingNeedsManualFix = !!cfg?.configured && ensureState === 'error' && !!ensureMessage;
    return (_jsxs("div", { children: [_jsx(Card, { size: "small", style: { marginBottom: 12 }, children: _jsxs(Space, { wrap: true, style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: "\u901A\u9053 C \u00B7 AI \u540C\u6B65\u5B66" }), _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 0, marginTop: 4 }, children: "\u6253\u5F00\u672C Tab \u65F6\uFF1A\u5DF2\u6B63\u786E\u7ED1\u5B9A\u5219\u81EA\u52A8\u540C\u6B65\u540D\u5355\uFF1B\u5C1A\u672A\u7ED1\u5B9A\u5219\u81EA\u52A8\u5F00\u901A\u3002\u82E5\u53D1\u73B0\u9519\u7ED1\u4E0D\u4F1A\u81EA\u52A8\u6539\u7ED1\uFF0C\u8BF7\u70B9\u53F3\u4E0A\u89D2\u624B\u52A8\u5904\u7406\u3002" })] }), _jsx(Button, { type: "primary", icon: _jsx(SettingOutlined, {}), onClick: () => setModalOpen(true), children: "\u540C\u6B65\u5B66\u914D\u7F6E" })] }) }), stillProvisioning ? (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u6B63\u5728\u81EA\u52A8\u5F00\u901A\u540C\u6B65\u5B66\u5E76\u540C\u6B65\u672C\u73ED\u5B66\u5458\u2026", description: "\u4F18\u5148\u590D\u7528\u300C\u73ED\u7EA7\u540D-\u540C\u6B65\u5B66\u300D\u8BA1\u5212\uFF1B\u82E5\u65E0\u5339\u914D\u5219\u65B0\u5EFA\u3002\u4E5F\u53EF\u70B9\u53F3\u4E0A\u89D2\u624B\u52A8\u6539\u7ED1\u3002" })) : provisionFailed ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u81EA\u52A8\u5F00\u901A\u540C\u6B65\u5B66\u5931\u8D25", description: ensureMessage || '请打开「同步学配置」手动绑定考试计划（可选择已有「班级名-同步学」）。' })) : bindingNeedsManualFix ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u5F53\u524D\u540C\u6B65\u5B66\u7ED1\u5B9A\u5F02\u5E38\uFF0C\u672A\u81EA\u52A8\u6539\u7ED1", description: ensureMessage || '请打开「同步学配置」手动选择正确考试计划，避免学情被悄悄换掉。' })) : provisionEmpty ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u672C\u73ED\u5C1A\u672A\u7ED1\u5B9A\u540C\u6B65\u5B66\u8003\u8BD5\u8BA1\u5212", description: "\u8003\u8BD5\u8BA1\u5212\u5217\u8868\u91CC\u82E5\u5DF2\u6709\u300C\u672C\u73ED\u540D-\u540C\u6B65\u5B66\u300D\uFF0C\u8BF7\u70B9\u53F3\u4E0A\u89D2\u300C\u540C\u6B65\u5B66\u914D\u7F6E\u300D\u624B\u52A8\u7ED1\u5B9A\uFF0C\u907F\u514D\u5B66\u60C5\u5BF9\u4E0D\u4E0A\u3002" })) : (_jsx(Card, { size: "small", loading: configQuery.isLoading, style: { marginBottom: 12 }, children: _jsxs(Descriptions, { column: 2, size: "small", children: [_jsxs(Descriptions.Item, { label: "\u8003\u8BD5\u8BA1\u5212", children: [cfg?.examTitle || cfg?.examId || '—', cfg?.examGone ? _jsx(Tag, { color: "error", style: { marginLeft: 8 }, children: "\u5DF2\u5931\u6548" }) : null] }), _jsxs(Descriptions.Item, { label: "\u72B6\u6001", children: [cfg?.enabled ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }), reasonLabel ? _jsx(Tag, { style: { marginLeft: 4 }, children: reasonLabel }) : null, resolve?.configured && !reasonLabel ? (_jsx(Tag, { color: "processing", style: { marginLeft: 4 }, children: "\u5165\u53E3\u53EF\u89E3\u6790" })) : null] }), _jsx(Descriptions.Item, { label: "\u5F00\u7EC3\u7A97\u53E3", children: cfg?.syncStartTime || cfg?.syncEndTime
                                ? `${cfg?.syncStartTime || '不限'} ~ ${cfg?.syncEndTime || '不限'}`
                                : '不限' }), _jsx(Descriptions.Item, { label: "\u7070\u5EA6", children: !cfg?.grayEnabled
                                ? _jsx(Tag, { color: "success", children: "\u5168\u91CF\u5F00\u653E" })
                                : cfg?.examInGray
                                    ? _jsx(Tag, { color: "success", children: "\u5DF2\u653E\u884C" })
                                    : _jsx(Tag, { color: "warning", children: "\u672A\u52A0\u5165\u767D\u540D\u5355" }) }), _jsxs(Descriptions.Item, { label: "\u540D\u5355\u5DEE\u989D", children: ["\u5728\u518C ", roster?.inExamPeople ?? 0, " / \u6709\u8D26\u53F7 ", roster?.withPlatformUser ?? 0, (roster?.missingEnroll ?? 0) > 0 && (_jsxs(Typography.Text, { type: "warning", style: { marginLeft: 8 }, children: ["\u5F85\u540C\u6B65 ", roster?.missingEnroll] }))] }), _jsx(Descriptions.Item, { label: "\u5B66\u60C5", children: !showStats
                                ? '—'
                                : canViewStats
                                    ? '见下方'
                                    : '无查看权限（需 exam:review-sync:stats:view）' })] }) })), bindingNeedsManualFix && cfg?.configured ? (_jsx(Card, { size: "small", loading: configQuery.isLoading, style: { marginBottom: 12 }, children: _jsxs(Descriptions, { column: 2, size: "small", children: [_jsxs(Descriptions.Item, { label: "\u5F53\u524D\u7ED1\u5B9A", children: [cfg?.examTitle || cfg?.examId || '—', cfg?.examGone ? _jsx(Tag, { color: "error", style: { marginLeft: 8 }, children: "\u5DF2\u5931\u6548" }) : null] }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: cfg?.enabled ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }) })] }) })) : null, showStats ? (_jsx(Card, { size: "small", style: { marginBottom: 12 }, title: null, children: canViewStats ? (_jsx(StatsOverviewPage, { examId: examIdNum, examTitle: cfg?.examTitle ?? undefined, embedded: true, enabled: active })) : (_jsx(Alert, { type: "info", showIcon: true, message: "\u6682\u65E0\u540C\u6B65\u5B66\u5B66\u60C5\u67E5\u770B\u6743\u9650", description: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u5F00\u901A\u6743\u9650\u7801 exam:review-sync:stats:view\u3002" })) })) : cfg?.configured && cfg?.examGone ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u8003\u8BD5\u8BA1\u5212\u5DF2\u5931\u6548\uFF0C\u6682\u65E0\u6CD5\u5C55\u793A\u540C\u6B65\u5B66\u5B66\u60C5", description: "\u8BF7\u6253\u5F00\u300C\u540C\u6B65\u5B66\u914D\u7F6E\u300D\u91CD\u65B0\u7ED1\u5B9A\u6709\u6548\u8BA1\u5212\u3002" })) : null, _jsx(Alert, { type: "info", showIcon: true, message: "\u65E0 App \u8D26\u53F7\u7684\u5B66\u5458\u4E0D\u4F1A\u5199\u5165\u8003\u751F\u540D\u5355\uFF1B\u82B1\u540D\u518C\u65B0\u589E\u6216\u8865\u5F00\u8D26\u53F7\u540E\u4F1A\u518D\u6B21\u81EA\u52A8\u540C\u6B65\u3002\u5237\u9898\u5B66\u60C5\u4E0E\u540C\u6B65\u5B66\u6307\u6807\u4ECD\u5206\u901A\u9053\u7EDF\u8BA1\u3002" }), _jsx(ClassReviewSyncModal, { open: modalOpen, onClose: () => {
                    setModalOpen(false);
                    void configQuery.refetch();
                }, classId: classId, classTitle: classTitle })] }));
}
