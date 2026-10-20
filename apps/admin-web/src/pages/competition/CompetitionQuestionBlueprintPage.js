import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ApartmentOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Modal, Select, Space, Table, Tag, Tree, Upload, message } from 'antd';
import { useEffect, useState } from 'react';
import { activateCompetitionQuestionBlueprint, archiveCompetitionQuestionBlueprint, downloadCompetitionQuestionBlueprintSource, downloadCompetitionQuestionBlueprintTemplate, getCompetitionQuestionBlueprintCoverage, getCompetitionQuestionBlueprintTree, importCompetitionQuestionBlueprint, pageCompetitionQuestionBlueprints, } from '../../api/competitionQuestionBlueprint';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents } from '../../api/competition';
function eventFromUrl() { return new URLSearchParams(window.location.search).get('eventId') || ''; }
export function getCompetitionQuestionBlueprintErrorMessage(error) {
    const candidate = error && typeof error === 'object' ? error : {};
    const responseData = candidate.response?.data;
    if (responseData && typeof responseData === 'object' && 'message' in responseData) {
        const responseMessage = responseData.message;
        if (typeof responseMessage === 'string' && responseMessage.trim())
            return responseMessage;
    }
    if (typeof responseData === 'string' && responseData.trim())
        return responseData;
    if (typeof candidate.message === 'string' && candidate.message.trim())
        return candidate.message;
    return '请求失败';
}
function checkReportLines(report) {
    if (!report)
        return [];
    try {
        const parsed = JSON.parse(report);
        return [
            ...(parsed.errors || []).map((item) => `错误：${item}`),
            ...(parsed.warnings || []).map((item) => `警告：${item}`),
        ];
    }
    catch {
        return [report];
    }
}
export function hasLiveCompetitionQuestionBlueprint(items) {
    return (items ?? []).some((item) => item.status === 'draft' || item.status === 'active');
}
export function CompetitionQuestionBlueprintPage({ contentType }) {
    const queryClient = useQueryClient();
    const [eventId, setEventId] = useState(eventFromUrl);
    const [workTypeId, setWorkTypeId] = useState('');
    const [levelId, setLevelId] = useState('');
    const [treeId, setTreeId] = useState(null);
    const [warningUpload, setWarningUpload] = useState(null);
    const [warningMessage, setWarningMessage] = useState(null);
    const eventsQuery = useQuery({ queryKey: ['competition-events', 'blueprints'], queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }) });
    const workTypesQuery = useQuery({ queryKey: ['competition-blueprints-work-types', eventId], queryFn: () => listCompetitionEventWorkTypes(eventId), enabled: Boolean(eventId) });
    const levelsQuery = useQuery({ queryKey: ['competition-blueprints-levels', eventId, workTypeId], queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId), enabled: Boolean(eventId && workTypeId) });
    const pageQuery = useQuery({
        queryKey: ['competition-blueprints', eventId, workTypeId, levelId, contentType],
        queryFn: () => pageCompetitionQuestionBlueprints(eventId, { page: 1, pageSize: 100, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType }),
        enabled: Boolean(eventId && workTypeId && levelId),
    });
    const treeQuery = useQuery({ queryKey: ['competition-blueprint-tree', eventId, treeId], queryFn: () => getCompetitionQuestionBlueprintTree(eventId, treeId), enabled: Boolean(eventId && treeId) });
    const coverageQuery = useQuery({ queryKey: ['competition-blueprint-coverage', eventId, treeId], queryFn: () => getCompetitionQuestionBlueprintCoverage(eventId, treeId), enabled: Boolean(eventId && treeId) });
    const importMutation = useMutation({
        mutationFn: (payload) => importCompetitionQuestionBlueprint(eventId, { eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, confirmWarnings: payload.confirmWarnings }, payload.file),
        onSuccess: () => { message.success('细目表已导入为草稿版本，请检查后启用'); queryClient.invalidateQueries({ queryKey: ['competition-blueprints'] }); setWarningUpload(null); setWarningMessage(null); },
        onError: (error) => {
            // The first request is deliberately sent without confirmation so the
            // server can return the exact title/metadata/XYZ warning. Keep the file
            // for the explicit second confirmation; close only hard validation errors.
            const errorMessage = getCompetitionQuestionBlueprintErrorMessage(error);
            if (errorMessage.includes('待确认警告')) {
                setWarningMessage(errorMessage);
            }
            else {
                setWarningUpload(null);
                setWarningMessage(null);
            }
            if (!errorMessage.includes('待确认警告'))
                message.error(errorMessage || '细目表导入失败');
        },
    });
    const activateMutation = useMutation({ mutationFn: (id) => activateCompetitionQuestionBlueprint(eventId, id, true), onSuccess: () => { message.success('细目表已启用'); queryClient.invalidateQueries({ queryKey: ['competition-blueprints'] }); } });
    const archiveMutation = useMutation({ mutationFn: (id) => archiveCompetitionQuestionBlueprint(eventId, id), onSuccess: () => { message.success('细目表已归档'); queryClient.invalidateQueries({ queryKey: ['competition-blueprints'] }); } });
    const events = eventsQuery.data?.items ?? [];
    const workTypes = workTypesQuery.data ?? [];
    const levels = levelsQuery.data ?? [];
    const blueprintRows = pageQuery.data?.items ?? [];
    const hasLiveBlueprint = hasLiveCompetitionQuestionBlueprint(blueprintRows);
    useEffect(() => { if (!eventId && events[0])
        setEventId(String(events[0].id)); }, [eventId, events]);
    useEffect(() => { if (!workTypeId && workTypes[0])
        setWorkTypeId(String(workTypes[0].id)); }, [workTypeId, workTypes]);
    useEffect(() => { if (!levelId && levels[0])
        setLevelId(String(levels[0].id)); }, [levelId, levels]);
    const uploadProps = {
        accept: '.xlsx,.xls', showUploadList: false,
        beforeUpload: (file) => {
            setWarningUpload(file);
            setWarningMessage(null);
            return Upload.LIST_IGNORE;
        },
    };
    const uploadConfirmed = () => {
        if (warningUpload) {
            importMutation.mutate({ file: warningUpload, confirmWarnings: Boolean(warningMessage) });
        }
    };
    const downloadSource = async (row) => {
        const blob = await downloadCompetitionQuestionBlueprintSource(eventId, row.id);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = row.sourceFileName || '细目表.xlsx';
        a.click();
        URL.revokeObjectURL(url);
    };
    const downloadTemplate = async () => {
        try {
            const blob = await downloadCompetitionQuestionBlueprintTemplate(contentType);
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = contentType === 'theory' ? '竞赛理论细目表导入模板.xlsx' : '竞赛实操细目表导入模板.xlsx';
            anchor.click();
            URL.revokeObjectURL(url);
        }
        catch (error) {
            message.error(error instanceof Error ? error.message : '导入模板下载失败');
        }
    };
    const activateRow = (row) => {
        const warnings = checkReportLines(row.checkReport).filter((line) => line.startsWith('警告：'));
        if (warnings.length === 0) {
            activateMutation.mutate(row.id);
            return;
        }
        Modal.confirm({
            title: '确认启用带警告的细目表？',
            content: _jsx(Space, { direction: "vertical", children: warnings.map((warning, index) => _jsx("span", { children: warning }, `${index}-${warning}`)) }),
            okText: '确认启用',
            cancelText: '返回检查',
            onOk: () => activateMutation.mutate(row.id),
        });
    };
    const archiveRow = (row) => {
        if (row.status !== 'active') {
            archiveMutation.mutate(row.id);
            return;
        }
        Modal.confirm({
            title: '确认归档当前启用的细目表？',
            content: '归档后，该工种该级别将没有启用中的细目表；题库导入和自动组卷需重新导入并启用后才能继续。已发布试卷不受影响。',
            okText: '确认归档',
            cancelText: '取消',
            okButtonProps: { danger: true },
            onOk: () => archiveMutation.mutate(row.id),
        });
    };
    const toTreeNode = (node) => ({
        key: node.id,
        title: `${node.fullCode} ${node.name}`,
        children: node.children?.map(toTreeNode),
    });
    const treeData = treeQuery.data?.tree.map(toTreeNode);
    const reportLines = checkReportLines(treeQuery.data?.blueprint.checkReport);
    return _jsxs(Card, { title: `竞赛${contentType === 'theory' ? '理论' : '实操'}细目表`, extra: _jsxs(Space, { wrap: true, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 240 }, placeholder: "\u9009\u62E9\u8D5B\u4E8B", value: eventId || undefined, options: events.map((e) => ({ value: String(e.id), label: `${e.name}（${e.code}）` })), onChange: (v) => { setEventId(v); setWorkTypeId(''); setLevelId(''); } }), _jsx(Select, { style: { width: 210 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", value: workTypeId || undefined, options: workTypes.map((w) => ({ value: String(w.id), label: w.displayName || w.workTypeName || String(w.workTypeId) })), onChange: (v) => { setWorkTypeId(v); setLevelId(''); } }), _jsx(Select, { style: { width: 170 }, placeholder: "\u9009\u62E9\u7B49\u7EA7", value: levelId || undefined, options: levels.map((l) => ({ value: String(l.id), label: `${l.levelName}（${l.levelCode}）` })), onChange: setLevelId }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => void downloadTemplate(), children: "\u4E0B\u8F7D\u5BFC\u5165\u6A21\u677F" }), _jsx(Upload, { ...uploadProps, disabled: !eventId || !workTypeId || !levelId || importMutation.isPending || hasLiveBlueprint, children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), disabled: hasLiveBlueprint, children: "\u5BFC\u5165\u7EC6\u76EE\u8868" }) })] }), children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u540C\u4E00\u8D5B\u4E8B\u5DE5\u79CD\u7B49\u7EA7\u53EA\u80FD\u6709\u4E00\u4EFD\u7406\u8BBA/\u5B9E\u64CD\u7EC6\u76EE\u8868\u3002\u5DF2\u6709\u8349\u7A3F\u6216\u542F\u7528\u7248\u672C\u65F6\u4E0D\u80FD\u91CD\u590D\u5BFC\u5165\uFF1B\u66F4\u6362\u65F6\u8BF7\u5148\u5F52\u6863\u73B0\u6709\u7EC6\u76EE\u8868\u3002\u542F\u7528\u540E\uFF0C\u9898\u5E93\u5BFC\u5165\u548C\u81EA\u52A8\u7EC4\u5377\u53EA\u4F7F\u7528\u8BE5\u4EFD\u672B\u7EA7\u8003\u8BC4\u70B9\u3002\u6807\u9898\u3001\u5143\u6570\u636E\u6216 XYZ \u6BD4\u4F8B\u8B66\u544A\u9700\u4EBA\u5DE5\u786E\u8BA4\u3002", style: { marginBottom: 16 } }), hasLiveBlueprint && _jsx(Alert, { type: "warning", showIcon: true, message: "\u5F53\u524D\u5DE5\u79CD\u7B49\u7EA7\u5DF2\u6709\u8349\u7A3F\u6216\u542F\u7528\u4E2D\u7684\u7EC6\u76EE\u8868\uFF0C\u4E0D\u80FD\u518D\u5BFC\u5165\u7B2C\u4E8C\u4EFD\u3002\u5982\u9700\u66F4\u6362\uFF0C\u8BF7\u5148\u5F52\u6863\u73B0\u6709\u7EC6\u76EE\u8868\u3002", style: { marginBottom: 16 } }), _jsx(Table, { rowKey: "id", loading: pageQuery.isLoading, dataSource: pageQuery.data?.items ?? [], pagination: false, scroll: { x: 'max-content' }, columns: [
                    { title: '版本', dataIndex: 'versionNo', width: 80, render: (v) => `v${v}` },
                    { title: '标题', dataIndex: 'title', ellipsis: true },
                    { title: '主题/考评点', key: 'counts', render: (_, row) => `${row.topicCount} / ${row.pointCount}` },
                    { title: 'XYZ', dataIndex: 'xyzRatio' },
                    { title: '状态', dataIndex: 'status', render: (v) => _jsx(Tag, { color: v === 'active' ? 'green' : v === 'draft' ? 'gold' : 'default', children: v === 'active' ? '启用' : v === 'draft' ? '草稿' : '已归档' }) },
                    { title: '操作', key: 'actions', render: (_, row) => _jsxs(Space, { children: [_jsx(Button, { type: "link", icon: _jsx(ApartmentOutlined, {}), onClick: () => setTreeId(row.id), children: "\u6811\u5F62\u9884\u89C8" }), _jsx(Button, { type: "link", icon: _jsx(DownloadOutlined, {}), onClick: () => void downloadSource(row), children: "\u6E90\u6587\u4EF6" }), row.status === 'draft' && _jsx(Button, { type: "link", onClick: () => activateRow(row), children: "\u542F\u7528" }), (row.status === 'draft' || row.status === 'active') && _jsx(Button, { type: "link", danger: true, onClick: () => archiveRow(row), children: "\u5F52\u6863" })] }) },
                ] }), _jsx(Modal, { title: warningMessage ? '确认细目表导入警告' : '校验细目表', open: Boolean(warningUpload), onCancel: () => { setWarningUpload(null); setWarningMessage(null); }, onOk: uploadConfirmed, okText: warningMessage ? '确认警告并导入' : '校验并导入', confirmLoading: importMutation.isPending, children: _jsx(Alert, { type: warningMessage ? 'warning' : 'info', showIcon: true, message: warningMessage || '系统会先执行层级、权重、末级考评点和元数据校验；如发现警告，会在下一步展示具体内容。' }) }), _jsx(Modal, { title: "\u7EC6\u76EE\u8868\u6811\u5F62\u9884\u89C8", open: Boolean(treeId), onCancel: () => setTreeId(null), footer: null, width: 980, children: treeQuery.data && _jsxs(_Fragment, { children: [_jsxs(Descriptions, { size: "small", column: 5, children: [_jsxs(Descriptions.Item, { label: "\u7248\u672C", children: ["v", treeQuery.data.blueprint.versionNo] }), _jsx(Descriptions.Item, { label: "\u8003\u8BC4\u70B9", children: treeQuery.data.blueprint.pointCount }), _jsx(Descriptions.Item, { label: "\u4E3B\u9898", children: treeQuery.data.blueprint.topicCount }), _jsx(Descriptions.Item, { label: "\u5DF2\u8986\u76D6\u8003\u8BC4\u70B9", children: coverageQuery.data?.coveredPoints ?? '-' }), _jsx(Descriptions.Item, { label: "\u5DF2\u7ED1\u5B9A\u9898\u76EE", children: coverageQuery.data?.boundQuestions ?? '-' })] }), reportLines.length > 0 && _jsx(Alert, { type: reportLines.some((line) => line.startsWith('错误：')) ? 'error' : 'warning', showIcon: true, message: "\u5BFC\u5165\u6821\u9A8C\u62A5\u544A", description: _jsx(Space, { direction: "vertical", children: reportLines.map((line, index) => _jsx("span", { children: line }, `${index}-${line}`)) }), style: { marginBottom: 12 } }), _jsx(Tree, { showLine: true, treeData: treeData }), _jsx(Table, { size: "small", style: { marginTop: 16 }, rowKey: "nodeId", loading: coverageQuery.isLoading, dataSource: coverageQuery.data?.points ?? [], pagination: { pageSize: 8 }, columns: [{ title: '考评点', dataIndex: 'path', ellipsis: true }, { title: '编码', dataIndex: 'fullCode', width: 140 }, { title: '题量要求', dataIndex: 'questionQuota', width: 90 }, { title: '已绑定', dataIndex: 'boundQuestionCount', width: 90 }, { title: '缺口', dataIndex: 'shortage', width: 80, render: (value) => value > 0 ? _jsx(Tag, { color: "red", children: value }) : _jsx(Tag, { color: "green", children: "0" }) }] })] }) })] });
}
