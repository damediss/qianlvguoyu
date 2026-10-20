import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { FolderOpenOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Empty, message, Modal, Space, Tabs, Typography } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { archiveBasic } from '../../api/certificationArchive';
import { PermissionGate } from '../../directives/permission';
import { ArchiveDetail } from './archive/ArchiveDetail';
import { ArchiveWorkbench } from './archive/ArchiveWorkbench';
import { BasicArchive } from './archive/BasicArchive';
import { CandidateMaterialImport } from './archive/CandidateMaterialImport';
import { TaskArchiveMaterials } from './archive/TaskArchiveMaterials';
export function ArchivePage() {
    const queryClient = useQueryClient();
    const [keyword, setKeyword] = useState();
    const [archiveId, setArchiveId] = useState();
    const [activeTab, setActiveTab] = useState('basic');
    const [detailOpen, setDetailOpen] = useState(false);
    const basicMutation = useMutation({
        mutationFn: (task) => archiveBasic(task.taskId),
        onSuccess: (snapshot) => {
            message.success('基础信息已归档');
            setArchiveId(snapshot.archiveId);
            setActiveTab('basic');
            queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
        },
    });
    const refreshAll = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-archive'] });
        queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
    };
    return (_jsx(PermissionGate, { code: "certification.archive.task.view", fallback: _jsx(Empty, { description: "\u6682\u65E0\u8BA4\u5B9A\u5F52\u6863\u8BBF\u95EE\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(FolderOpenOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u8BA4\u5B9A\u5F52\u6863" }), _jsx(Typography.Text, { type: "secondary", children: "\u5DF2\u5B8C\u6210\u8BA4\u5B9A\u4EFB\u52A1\u7684\u57FA\u7840\u5FEB\u7167\u3001\u6750\u6599\u5F52\u96C6\u548C\u6587\u4EF6\u8BBF\u95EE" })] })] }), archiveId && _jsxs(Typography.Text, { type: "secondary", children: ["\u5F53\u524D\u5F52\u6863\u8BB0\u5F55\uFF1A", archiveId] })] }), _jsx(Space, { children: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refreshAll, children: "\u5237\u65B0" }) })] }), _jsx(Card, { title: "\u4EFB\u52A1\u680F", size: "small", style: { marginBottom: 16 }, children: _jsx(ArchiveWorkbench, { keyword: keyword, onKeywordChange: setKeyword, onArchiveBasic: (task) => basicMutation.mutate(task), onSelectArchive: (id) => {
                            setArchiveId(id);
                            setDetailOpen(true);
                        } }) }), _jsx(Card, { size: "small", children: _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                            { key: 'basic', label: '基础信息归档', children: _jsx(BasicArchive, { archiveId: archiveId }) },
                            { key: 'materials', label: '认定材料', children: _jsx(TaskArchiveMaterials, { archiveId: archiveId }) },
                            { key: 'import', label: '考生材料导入', children: _jsx(CandidateMaterialImport, { archiveId: archiveId }) },
                        ] }) }), _jsx(Modal, { title: "\u8BA4\u5B9A\u5F52\u6863\u8BE6\u60C5", open: detailOpen, width: 1120, footer: null, destroyOnHidden: true, onCancel: () => setDetailOpen(false), children: _jsx(ArchiveDetail, { archiveId: archiveId, onCompleted: () => {
                            setDetailOpen(false);
                            setArchiveId(undefined);
                            setActiveTab('basic');
                            refreshAll();
                        } }) })] }) }));
}
