import { jsx as _jsx } from "react/jsx-runtime";
import { Alert, Card, Result, Spin, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { getCompetitionStageWorkspace, replaceCompetitionStageContents } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import { StageContentPanel } from './stage-planning/StageContentPanel';
function errorText(error) {
    return error instanceof Error ? error.message : '操作失败，请刷新后重试';
}
export function CompetitionStageExecutionWorkbenchPage({ stage }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:stage-plan:view');
    const canManage = permissions.includes('competition:stage-plan:manage');
    const stageWorkspaceQuery = useQuery({
        queryKey: ['competition-stage-workspace', stage.id],
        queryFn: () => getCompetitionStageWorkspace(stage.id),
        enabled: canView,
    });
    const contentSaveMutation = useMutation({
        mutationFn: (contents) => replaceCompetitionStageContents(stage.id, contents.map(({ contentType, weight }) => ({
            contentType: contentType,
            status: 'enabled',
            weight,
            providerCode: contentType === 'simulation' ? 'simulation-placeholder' : undefined,
        }))),
        onSuccess: async () => {
            await stageWorkspaceQuery.refetch();
            message.success('赛段内容与权重已保存');
        },
        onError: (error) => message.error(errorText(error)),
    });
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u6267\u884C\u5DE5\u4F5C\u53F0\u67E5\u770B\u6743\u9650" });
    if (stageWorkspaceQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (stageWorkspaceQuery.isError)
        return _jsx(Alert, { type: "error", message: "\u8D5B\u6BB5\u5185\u5BB9\u52A0\u8F7D\u5931\u8D25" });
    const workspaceStage = stageWorkspaceQuery.data?.stage || stage;
    const stageEditable = canManage && (workspaceStage.status === 'draft' || workspaceStage.status === 'configuring');
    return (_jsx(Card, { bordered: false, style: { marginTop: 16 }, children: _jsx(StageContentPanel, { stage: workspaceStage, contents: stageWorkspaceQuery.data?.contents || [], contentEditable: stageEditable, contentSaving: contentSaveMutation.isPending, onSave: (contents) => contentSaveMutation.mutate(contents) }) }));
}
