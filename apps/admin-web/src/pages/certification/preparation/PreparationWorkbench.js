import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckCircleOutlined, } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Descriptions, Empty, List, Modal, Progress, Space, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { completePreparation, getPreparationCompleteCheck, getPreparationTaskOverview, } from '../../../api/certificationPreparation';
import { PermissionGate } from '../../../directives/permission';
import { dateText, dateTimeText, statusTag } from './shared';
export function PreparationWorkbench({ taskId, task, onChanged }) {
    const queryClient = useQueryClient();
    const overviewQuery = useQuery({
        queryKey: ['certification-preparation-overview', taskId],
        queryFn: () => getPreparationTaskOverview(taskId),
        enabled: Boolean(taskId),
    });
    const overview = overviewQuery.data || task;
    const checkMutation = useMutation({
        mutationFn: () => getPreparationCompleteCheck(taskId),
        onSuccess: (result) => showCheckResult(result, false),
    });
    const completeMutation = useMutation({
        mutationFn: () => completePreparation(taskId),
        onSuccess: (result) => {
            showCheckResult(result, true);
            queryClient.invalidateQueries({ queryKey: ['certification-preparation-tasks'] });
            queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
            queryClient.invalidateQueries({ queryKey: ['certification-tasks'] });
            onChanged?.();
        },
    });
    if (!taskId || !overview) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u67E5\u770B\u7B79\u5907\u5DE5\u4F5C\u53F0" });
    }
    // 是否已经离开“认定筹备”阶段：进入归档或认定完成阶段
    const preparationCompleted = ['archive', 'certification_completed'].includes(overview.currentPhase || '');
    // 只有任务当前阶段=preparation 时才允许触发“完成筹备”
    const canCompletePreparation = overview.currentPhase === 'preparation';
    return (_jsx(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: _jsxs(ProCard, { title: "\u7B79\u5907\u6982\u89C8", extra: _jsxs(Space, { children: [_jsx(Button, { loading: checkMutation.isPending, onClick: () => checkMutation.mutate(), children: "\u7B79\u5907\u5B8C\u6210\u68C0\u67E5" }), _jsx(PermissionGate, { code: "certification.preparation.complete", children: _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), loading: completeMutation.isPending, disabled: preparationCompleted || !canCompletePreparation, onClick: () => completeMutation.mutate(), children: preparationCompleted ? '已完成筹备' : '完成筹备' }) })] }), children: [_jsxs(Descriptions, { bordered: true, size: "small", column: 4, children: [_jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7F16\u53F7", children: overview.taskNo }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u540D\u79F0", span: 2, children: overview.taskName }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u9636\u6BB5", children: statusTag(overview.currentPhase) }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u72B6\u6001", children: statusTag(overview.status) }), _jsx(Descriptions.Item, { label: "\u6279\u6B21", children: overview.batchName || '-' }), _jsx(Descriptions.Item, { label: "\u5DE5\u79CD", children: overview.workTypeName || '-' }), _jsx(Descriptions.Item, { label: "\u7B49\u7EA7", children: overview.levelName || '-' }), _jsx(Descriptions.Item, { label: "\u6750\u6599\u72B6\u6001", children: statusTag(overview.materialStatus) }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u5F00\u59CB", children: dateText(overview.planStartDate) }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u7ED3\u675F", children: dateText(overview.planEndDate) }), _jsx(Descriptions.Item, { label: "\u6700\u540E\u66F4\u65B0", span: 2, children: dateTimeText(overview.lastUpdatedAt) })] }), _jsxs("div", { style: { marginTop: 16 }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u8D44\u6599\u5B8C\u6210\u7387" }), _jsx(Progress, { percent: overview.documentCompletionRate || 0 })] })] }) }));
}
function showCheckResult(result, completionAttempt) {
    const title = result.passed
        ? completionAttempt ? '筹备已完成' : '筹备检查通过'
        : completionAttempt ? '筹备尚未满足完成条件' : '筹备检查未通过';
    if (result.passed && completionAttempt) {
        message.success('认定筹备已完成，任务可进入后续实施阶段');
    }
    Modal.info({
        title,
        width: 680,
        content: (_jsx(List, { size: "small", dataSource: result.items || [], renderItem: (item) => (_jsx(List.Item, { children: _jsxs(Space, { children: [item.passed ? _jsx(CheckCircleOutlined, { style: { color: '#16a34a' } }) : _jsx(CheckCircleOutlined, { style: { color: '#dc2626' } }), _jsx(Typography.Text, { strong: true, children: item.name }), _jsx(Typography.Text, { type: item.passed ? 'secondary' : 'danger', children: item.message })] }) })) })),
    });
}
