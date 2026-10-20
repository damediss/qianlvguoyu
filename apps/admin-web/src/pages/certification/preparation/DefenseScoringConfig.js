import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { FileProtectOutlined, MobileOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Empty, Modal, Select, Space, Spin, Table, Tag, Typography, Upload, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { logout } from '../../../api/auth';
import { bindDefenseCandidateReport, deleteDefenseCandidateReport, getDefenseScoringSetup, matchDefenseReports, saveDefenseScoringConfig, } from '../../../api/defenseScoring';
import { fetchPlatformFileBlob, uploadPlatformFile } from '../../../api/files';
import { PermissionGate } from '../../../directives/permission';
import { useAuthStore } from '../../../stores/authStore';
export function DefenseScoringConfig({ taskId }) {
    const queryClient = useQueryClient();
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const [groupId, setGroupId] = useState();
    const [preview, setPreview] = useState();
    const [unmatched, setUnmatched] = useState([]);
    const setupQuery = useQuery({
        queryKey: ['defense-scoring-setup', taskId],
        queryFn: () => getDefenseScoringSetup(taskId),
        enabled: Boolean(taskId),
    });
    const setup = setupQuery.data;
    const uploadedCount = useMemo(() => (setup?.candidates || []).filter((row) => row.fileId).length, [setup?.candidates]);
    useEffect(() => {
        if (setup?.config?.groupId) {
            setGroupId(setup.config.groupId);
        }
    }, [setup?.config?.groupId]);
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['defense-scoring-setup', taskId] });
    };
    const saveConfigMutation = useMutation({
        mutationFn: (selectedGroupId) => saveDefenseScoringConfig(taskId, selectedGroupId),
        onSuccess: () => {
            message.success('已绑定综合答辩考评组');
            refresh();
        },
    });
    const matchMutation = useMutation({
        mutationFn: async (files) => {
            const uploaded = [];
            for (const file of files) {
                const stored = await uploadPlatformFile(file);
                uploaded.push({ fileId: String(stored.id), fileName: file.name });
            }
            return matchDefenseReports(taskId, uploaded);
        },
        onSuccess: (result) => {
            const parts = [`成功匹配 ${result.matched} 份`];
            if (result.replaced)
                parts.push(`替换 ${result.replaced} 份`);
            if (result.unmatched)
                parts.push(`未匹配 ${result.unmatched} 份`);
            message.success(parts.join('，'));
            setUnmatched(result.unmatchedFiles || []);
            refresh();
        },
    });
    const bindMutation = useMutation({
        mutationFn: async ({ candidateId, file }) => {
            const stored = await uploadPlatformFile(file);
            return bindDefenseCandidateReport(taskId, candidateId, {
                fileId: String(stored.id),
                fileName: file.name,
            });
        },
        onSuccess: () => {
            message.success('已上传业绩评估报告');
            refresh();
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (candidateId) => deleteDefenseCandidateReport(taskId, candidateId),
        onSuccess: () => {
            message.success('已删除业绩评估报告');
            refresh();
        },
    });
    if (!taskId) {
        return _jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u3002" });
    }
    const columns = [
        { title: '考生', dataIndex: 'candidateName', width: 110, fixed: 'left' },
        { title: '证件号', dataIndex: 'maskedIdCardNo', width: 180, render: (v, row) => v || row.idCardNo || '-' },
        { title: '工种', dataIndex: 'workTypeName', width: 120, render: (v) => v || '-' },
        { title: '等级', dataIndex: 'levelName', width: 100, render: (v) => v || '-' },
        { title: '单位', dataIndex: 'orgName', width: 160, ellipsis: true, render: (v) => v || '-' },
        {
            title: '业绩评估报告',
            width: 260,
            render: (_, row) => (row.fileId
                ? _jsx(Tag, { color: "success", children: row.fileName || '已上传 PDF' })
                : _jsx(Tag, { children: "\u672A\u4E0A\u4F20" })),
        },
        {
            title: '操作',
            width: 240,
            render: (_, row) => (_jsxs(Space, { wrap: true, children: [_jsx(PermissionGate, { code: "certification.preparation.task.view", children: _jsx(Upload, { accept: ".pdf,application/pdf", showUploadList: false, beforeUpload: (file) => {
                                if (!isPdfFile(file)) {
                                    message.error('仅支持 PDF 文件');
                                    return false;
                                }
                                bindMutation.mutate({ candidateId: row.candidateId, file });
                                return false;
                            }, children: _jsx(Button, { size: "small", icon: _jsx(UploadOutlined, {}), loading: bindMutation.isPending, children: row.fileId ? '替换' : '上传' }) }) }), row.fileId ? (_jsx(Button, { size: "small", onClick: () => setPreview(row), children: "\u9884\u89C8" })) : null, row.fileId ? (_jsx(PermissionGate, { code: "certification.preparation.task.view", children: _jsx(Button, { size: "small", danger: true, onClick: () => deleteMutation.mutate(row.candidateId), children: "\u5220\u9664" }) })) : null] })),
        },
    ];
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(ProCard, { title: (_jsxs(Space, { children: [_jsx(FileProtectOutlined, {}), "\u7EFC\u5408\u7B54\u8FA9\u914D\u7F6E"] })), extra: (_jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), _jsx(Button, { icon: _jsx(MobileOutlined, {}), onClick: async () => {
                                try {
                                    await logout();
                                }
                                catch {
                                    // 切换入口时仍清理登录态
                                }
                                finally {
                                    clearAuth();
                                    window.location.assign('/assessor-defense');
                                }
                            }, children: "\u6253\u5F00\u7EFC\u5408\u7B54\u8FA9\u7AEF" })] })), children: _jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u672C\u4EFB\u52A1\u7EFC\u5408\u7B54\u8FA9\u8003\u8BC4\u7EC4\u540E\uFF0C\u6309\u8003\u751F\u5339\u914D\u4E0A\u4F20\u4E1A\u7EE9\u8BC4\u4F30\u62A5\u544A PDF\u3002\u8003\u8BC4\u5458\u4F7F\u7528\u8003\u8BC4\u5458\u4FE1\u606F\u4E2D\u521B\u5EFA\u7684\u8D26\u53F7\uFF0C\u4ECE\u7EFC\u5408\u7B54\u8FA9\u7AEF\u7AD6\u5C4F\u9605\u89C8\u62A5\u544A\uFF0C\u5E76\u5B8C\u6210\u8BBA\u6587\u7B54\u8FA9\u8BC4\u5206\u4E0E\u4E1A\u7EE9\u8BC4\u4F30\u62A5\u544A\u8BC4\u5206\u3002" }), _jsx(Alert, { type: "info", showIcon: true, message: "\u4EBA\u5458\u540D\u5355\u9650\u5236\uFF1A\u4EC5\u6280\u5E08\u3001\u9AD8\u7EA7\u6280\u5E08\u53EF\u53C2\u52A0\u7EFC\u5408\u7B54\u8FA9", description: "\u521D\u7EA7\u3001\u4E2D\u7EA7\u3001\u9AD8\u7EA7\u5DE5\u4E0D\u8FDB\u5165\u672C\u540D\u5355\u3002\u7B54\u8FA9\u7BA1\u7406\u5C06\u540C\u6B65\u663E\u793A\u540C\u4E00\u6279\u8003\u751F\u3002" }), _jsxs(Space, { wrap: true, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u8003\u8BC4\u7EC4", value: groupId, loading: setupQuery.isFetching, style: { minWidth: 320 }, options: (setup?.groups || []).map((group) => ({
                                        value: group.id,
                                        disabled: group.memberCount < 1,
                                        label: `${group.groupName || '未命名考评组'}（${group.memberCount}人${group.leaderAssessorName ? ` · 组长${group.leaderAssessorName}` : ''}）`,
                                    })), onChange: setGroupId }), _jsx(PermissionGate, { code: "certification.preparation.task.view", children: _jsx(Button, { type: "primary", loading: saveConfigMutation.isPending, onClick: () => {
                                            if (!groupId) {
                                                message.warning('请选择考评组');
                                                return;
                                            }
                                            saveConfigMutation.mutate(groupId);
                                        }, children: "\u4FDD\u5B58\u8003\u8BC4\u7EC4" }) }), setup?.config?.groupName ? _jsxs(Tag, { color: "processing", children: ["\u5F53\u524D\uFF1A", setup.config.groupName] }) : null] })] }) }), _jsx(ProCard, { title: `业绩评估报告（已匹配 ${uploadedCount}/${setup?.candidates.length || 0}）`, extra: (_jsx(PermissionGate, { code: "certification.preparation.task.view", children: _jsx(Upload, { accept: ".pdf,application/pdf", multiple: true, showUploadList: false, beforeUpload: (file, fileList) => {
                            if (file !== fileList[fileList.length - 1]) {
                                return false;
                            }
                            const pdfs = fileList.filter((item) => isPdfFile(item));
                            if (pdfs.length !== fileList.length) {
                                message.warning('已忽略非 PDF 文件');
                            }
                            if (pdfs.length === 0) {
                                message.error('请选择 PDF 文件');
                                return false;
                            }
                            matchMutation.mutate(pdfs);
                            return false;
                        }, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), loading: matchMutation.isPending, children: "\u6309\u8003\u751F\u5339\u914D\u4E0A\u4F20" }) }) })), children: _jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u6279\u91CF\u4E0A\u4F20\u65F6\uFF0C\u6587\u4EF6\u540D\u9700\u5305\u542B\u8003\u751F\u8EAB\u4EFD\u8BC1\u53F7\u3001\u51C6\u8003\u8BC1\u53F7\u6216\u59D3\u540D\uFF0C\u7CFB\u7EDF\u4F1A\u81EA\u52A8\u5339\u914D\u5230\u5BF9\u5E94\u8003\u751F\u3002" }), unmatched.length > 0 ? (_jsx(Alert, { type: "warning", showIcon: true, message: "\u4EE5\u4E0B\u6587\u4EF6\u672A\u80FD\u5339\u914D", description: unmatched.map((item) => `${item.fileName || '未命名文件'}：${item.reason}`).join('；') })) : null, _jsx(Table, { rowKey: "candidateId", columns: columns, dataSource: setup?.candidates || [], loading: setupQuery.isFetching, pagination: { pageSize: 10, showSizeChanger: false }, scroll: { x: 1100 }, locale: { emptyText: '暂无符合条件的考生（仅技师、高级技师可参加综合答辩）' } })] }) }), _jsx(Modal, { title: preview ? `${preview.candidateName} · 业绩评估报告` : '预览', open: Boolean(preview), onCancel: () => setPreview(undefined), footer: null, width: 920, destroyOnHidden: true, children: preview?.fileId ? _jsx(DefensePdfFrame, { fileId: preview.fileId }) : _jsx(Empty, { description: "\u6682\u65E0\u6587\u4EF6" }) })] }));
}
function isPdfFile(file) {
    return file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
}
export function DefensePdfFrame({ fileId }) {
    const query = useQuery({
        queryKey: ['defense-pdf', fileId],
        queryFn: async () => {
            const blob = await fetchPlatformFileBlob(fileId);
            return URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
        },
        enabled: Boolean(fileId),
    });
    if (query.isLoading) {
        return _jsx(Spin, { style: { display: 'block', margin: '48px auto' } });
    }
    if (!query.data) {
        return _jsx(Empty, { description: "\u62A5\u544A\u52A0\u8F7D\u5931\u8D25" });
    }
    return (_jsx("iframe", { title: "\u4E1A\u7EE9\u8BC4\u4F30\u62A5\u544A", src: `${query.data}#toolbar=1&navpanes=0`, style: { width: '100%', height: '70vh', border: 0, background: '#525659' } }));
}
