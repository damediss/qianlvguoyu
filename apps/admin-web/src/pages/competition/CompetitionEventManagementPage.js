import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ArrowLeftOutlined, EditOutlined, EyeOutlined, FileTextOutlined, CopyOutlined, DownloadOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SaveOutlined, SafetyCertificateOutlined, CheckCircleOutlined, } from '@ant-design/icons';
import { Button, Card, DatePicker, Descriptions, Form, Input, Modal, Result, Select, Space, Spin, Table, Tag, Typography, message, } from 'antd';
import dayjs from 'dayjs';
import QRCode from 'qrcode';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { createCompetitionEvent, createCompetitionEventWorkType, deleteCompetitionEventWorkType, getCompetitionEvent, listCompetitionEventWorkTypes, listCompetitionEventWorkTypeLevels, pageCompetitionEvents, publishCompetitionEvent, replaceCompetitionEventWorkTypeLevels, updateCompetitionEventWorkType, updateCompetitionEvent, } from '../../api/competition';
import { treeWorkTypes } from '../../api/exam';
import { useAuthStore } from '../../stores/authStore';
import { useNavigationBlocker } from '../../hooks/useUnsavedChangesBlocker';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import { CompetitionEventOrganizationsPage } from './CompetitionEventOrganizationsPage';
import { CompetitionRosterImportPage } from './CompetitionRosterImportPage';
import { CompetitionPlayerManagementPage } from './CompetitionPlayerManagementPage';
import { CompetitionNoticePage } from './CompetitionNoticePage';
import { CompetitionScoreRankingPage } from './CompetitionScoreRankingPage';
import { CompetitionTheoryExamPage } from './CompetitionTheoryExamPage';
import { CompetitionEventModulePage } from './CompetitionEventModulePage';
const scopeOptions = [
    { label: '平台赛事', value: 'platform' },
    { label: '企业内部赛', value: 'enterprise_internal' },
];
const competitionLevelOptions = [
    { value: '5', label: '初级工' },
    { value: '4', label: '中级工' },
    { value: '3', label: '高级工' },
    { value: '2', label: '技师' },
    { value: '1', label: '高级技师' },
];
const statusOptions = [
    { label: '创建中', value: 'draft' },
    { label: '已发布', value: 'published' },
    { label: '已关闭', value: 'closed' },
    { label: '已归档', value: 'archived' },
];
const statusMeta = {
    draft: { label: '创建中', color: 'default' },
    published: { label: '已发布', color: 'success' },
    closed: { label: '已关闭', color: 'warning' },
    archived: { label: '已归档', color: 'error' },
};
function eventRoute() {
    const modulePathMap = {
        '/competition/stages': 'stages',
        '/competition/notices': 'notices',
        '/competition/theory-exams': 'theory-exam',
        '/competition/formal-exam': 'formal-exam-console',
        '/competition/practical-exam': 'practical-exam-console',
        '/competition/scores': 'scores',
    };
    const module = modulePathMap[window.location.pathname.replace(/\/$/, '')];
    if (module)
        return { mode: 'module', module };
    if (window.location.pathname.replace(/\/$/, '') === '/competition/players') {
        return { mode: 'players' };
    }
    const theoryExamMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/theory-exam\/?$/);
    if (theoryExamMatch?.[1]) {
        return { mode: 'theory-exam', eventId: decodeURIComponent(theoryExamMatch[1]) };
    }
    const scoresMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/scores\/?$/);
    if (scoresMatch?.[1]) {
        return { mode: 'scores', eventId: decodeURIComponent(scoresMatch[1]) };
    }
    const noticesMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/notices\/?$/);
    if (noticesMatch?.[1]) {
        return { mode: 'notices', eventId: decodeURIComponent(noticesMatch[1]) };
    }
    const rostersMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/rosters\/?$/);
    if (rostersMatch?.[1]) {
        return { mode: 'rosters', eventId: decodeURIComponent(rostersMatch[1]) };
    }
    const organizationsMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/organizations\/?$/);
    if (organizationsMatch?.[1]) {
        return { mode: 'organizations', eventId: decodeURIComponent(organizationsMatch[1]) };
    }
    const match = window.location.pathname.match(/^\/competition\/events(?:\/([^/]+))?(?:\/(edit))?\/?$/);
    if (!match?.[1]) {
        return { mode: 'list' };
    }
    if (match[1] === 'new') {
        return { mode: 'create' };
    }
    const eventId = match[1];
    return { mode: match[2] ? 'edit' : 'detail', eventId: decodeURIComponent(eventId) };
}
function idValue(id) {
    return String(id);
}
function formatDate(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}
function scopeLabel(scope) {
    return scopeOptions.find((item) => item.value === scope)?.label || scope || '-';
}
function statusTag(status) {
    const meta = status ? statusMeta[status] : undefined;
    return _jsx(Tag, { color: meta?.color, children: meta?.label || status || '-' });
}
function dateTimeValue(value) {
    return value ? dayjs(value) : undefined;
}
function toDateTime(value) {
    return value?.format('YYYY-MM-DDTHH:mm:ss');
}
async function copyText(value, successText) {
    try {
        await navigator.clipboard.writeText(value);
        message.success(successText);
    }
    catch {
        message.error('复制失败，请手动复制');
    }
}
function playerEntryQrPayload(entryCode) {
    return `qianlvguoyu-player://competition/activate?entry=${encodeURIComponent(entryCode)}`;
}
function downloadDataUrl(dataUrl, fileName) {
    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
}
export function CompetitionEventManagementPage() {
    const [route, setRoute] = useState(eventRoute);
    const { confirmNavigation } = useNavigationBlocker();
    const currentPathRef = useRef(window.location.pathname);
    useEffect(() => {
        const onPopState = () => {
            const nextPath = window.location.pathname;
            void confirmNavigation().then((allowed) => {
                if (!allowed) {
                    window.history.pushState({}, '', currentPathRef.current);
                    setRoute(eventRoute());
                    return;
                }
                currentPathRef.current = nextPath;
                setRoute(eventRoute());
            });
        };
        window.addEventListener('popstate', onPopState);
        return () => window.removeEventListener('popstate', onPopState);
    }, [confirmNavigation]);
    const navigate = async (path) => {
        if (!(await confirmNavigation()))
            return;
        window.history.pushState({}, '', path);
        currentPathRef.current = path;
        setRoute(eventRoute());
    };
    if (route.mode === 'list') {
        return _jsx(CompetitionEventListPage, { onNavigate: navigate });
    }
    if (route.mode === 'create') {
        return _jsx(CompetitionEventListPage, { onNavigate: navigate, initialCreateOpen: true });
    }
    if (route.mode === 'edit') {
        return _jsx(CompetitionEventListPage, { onNavigate: navigate, initialEditEventId: route.eventId });
    }
    if (route.mode === 'detail') {
        return _jsx(CompetitionEventListPage, { onNavigate: navigate, initialDetailEventId: route.eventId });
    }
    if (route.mode === 'module') {
        return _jsx(CompetitionEventModulePage, { module: route.module, onNavigate: navigate });
    }
    if (route.mode === 'players') {
        return _jsx(CompetitionPlayerManagementPage, {});
    }
    if (route.mode === 'notices' || route.mode === 'theory-exam' || route.mode === 'scores') {
        return _jsx(CompetitionEventModulePage, { module: route.mode, onNavigate: navigate, initialEventId: route.eventId });
    }
    if (route.mode === 'organizations' || route.mode === 'rosters') {
        return _jsx(CompetitionEventListPage, { onNavigate: navigate, initialWorkspaceRoute: route });
    }
    return null;
}
function CompetitionEventListPage({ onNavigate, initialCreateOpen = false, initialEditEventId, initialDetailEventId, initialWorkspaceRoute, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:event:view');
    const canCreate = permissions.includes('competition:event:create');
    const [keywordInput, setKeywordInput] = useState('');
    const [query, setQuery] = useState({ page: 1, pageSize: 10 });
    const [createOpen, setCreateOpen] = useState(initialCreateOpen);
    const [editingEventId, setEditingEventId] = useState(initialEditEventId);
    const [detailEventId, setDetailEventId] = useState(initialDetailEventId);
    const [workspaceRoute, setWorkspaceRoute] = useState(initialWorkspaceRoute);
    const queryClient = useQueryClient();
    useEffect(() => {
        setCreateOpen(initialCreateOpen);
        setEditingEventId(initialEditEventId);
        setDetailEventId(initialDetailEventId);
        setWorkspaceRoute(initialWorkspaceRoute);
    }, [initialCreateOpen, initialDetailEventId, initialEditEventId, initialWorkspaceRoute]);
    const listQuery = useQuery({
        queryKey: ['competition-events', query],
        queryFn: () => pageCompetitionEvents(query),
        enabled: canView,
    });
    const events = listQuery.data?.items || [];
    const columns = useMemo(() => [
        {
            title: '名称',
            dataIndex: 'name',
            render: (value, record) => (_jsx(Button, { type: "link", className: "competition-event-name", onClick: () => setDetailEventId(idValue(record.id)), children: value })),
        },
        { title: '编码', dataIndex: 'code', width: 180 },
        { title: '范围', dataIndex: 'scope', width: 130, render: scopeLabel },
        { title: '状态', dataIndex: 'status', width: 110, render: statusTag },
        { title: '开始时间', dataIndex: 'startAt', width: 170, render: formatDate },
        { title: '结束时间', dataIndex: 'endAt', width: 170, render: formatDate },
        {
            title: '操作',
            key: 'actions',
            width: 150,
            render: (_, record) => (_jsx(Space, { size: 0, children: _jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => setDetailEventId(idValue(record.id)), children: "\u8BE6\u60C5" }) })),
        },
    ], [onNavigate]);
    const closeFormModal = () => {
        setCreateOpen(false);
        setEditingEventId(undefined);
    };
    const closeDetailModal = () => setDetailEventId(undefined);
    const closeActiveModal = () => {
        closeFormModal();
        closeDetailModal();
        setWorkspaceRoute(undefined);
    };
    const handleFormNavigate = (path) => {
        closeActiveModal();
        onNavigate(path);
    };
    const handleDetailNavigate = (path) => {
        onNavigate(path);
    };
    const modalOpen = createOpen || Boolean(editingEventId) || Boolean(detailEventId) || Boolean(workspaceRoute);
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u4E8B\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7 competition:event:view \u6743\u9650\u3002" });
    }
    return (_jsxs("div", { className: "competition-page", children: [_jsx("div", { className: "competition-list-actions", children: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['competition-events'] }), children: "\u5237\u65B0" }), canCreate && (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => setCreateOpen(true), children: "\u521B\u5EFA\u8D5B\u4E8B" }))] }) }), _jsxs(Card, { bordered: false, className: "competition-filter-card competition-filter-card--workspace", children: [_jsxs("div", { className: "competition-filter-card__head", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u8BA1\u5212\u5217\u8868" }), _jsx(Typography.Text, { type: "secondary", children: "\u70B9\u51FB\u8BA1\u5212\u540D\u79F0\u8FDB\u5165\u8D5B\u4E8B\u5DE5\u4F5C\u53F0" })] }), _jsxs(Tag, { color: "blue", children: [_jsx(SafetyCertificateOutlined, {}), " \u6743\u9650\u5DF2\u6309\u83DC\u5355\u63A7\u5236"] })] }), _jsxs(Space, { wrap: true, className: "competition-filter-card__body", children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u8D5B\u4E8B\u540D\u79F0\u6216\u7F16\u7801", value: keywordInput, onChange: (event) => setKeywordInput(event.target.value), onPressEnter: () => setQuery((current) => ({ ...current, page: 1, keyword: keywordInput.trim() || undefined })), style: { width: 280 } }), _jsx(Select, { allowClear: true, placeholder: "\u6309\u72B6\u6001\u7B5B\u9009", options: statusOptions, value: query.status, onChange: (status) => setQuery((current) => ({ ...current, page: 1, status })), style: { width: 150 } }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(SearchOutlined, {}), onClick: () => setQuery((current) => ({ ...current, page: 1, keyword: keywordInput.trim() || undefined })), children: "\u641C\u7D22" })] })] }), _jsx(Card, { bordered: false, className: "competition-table-card competition-plan-table-card", children: _jsx(Table, { rowKey: (record) => idValue(record.id), loading: listQuery.isLoading, dataSource: events, columns: columns, scroll: { x: 1050 }, pagination: {
                        current: query.page,
                        pageSize: query.pageSize,
                        total: listQuery.data?.total || 0,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条`,
                        onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })),
                    } }) }), _jsx(Modal, { title: editingEventId ? '编辑竞赛计划' : detailEventId ? '赛事详情' : workspaceRoute ? competitionWorkspaceTitle(workspaceRoute.mode) : '创建竞赛计划', open: modalOpen, centered: true, width: workspaceRoute?.mode === 'scores' || workspaceRoute?.mode === 'theory-exam' ? 1320 : 920, footer: null, destroyOnClose: true, className: "competition-create-modal", onCancel: closeActiveModal, children: detailEventId ? (_jsx(CompetitionEventDetailPage, { eventId: detailEventId, onNavigate: handleDetailNavigate, inModal: true })) : workspaceRoute ? (_jsx("div", { className: "competition-detail-modal-content", children: _jsx(CompetitionEventWorkspaceModalContent, { route: workspaceRoute, onNavigate: handleDetailNavigate }) })) : (_jsx(CompetitionEventFormPage, { eventId: editingEventId, onNavigate: handleFormNavigate, inModal: true, onClose: closeFormModal })) })] }));
}
function competitionWorkspaceTitle(mode) {
    return {
        organizations: '参与企业',
        rosters: '名单导入',
        notices: '公告设置',
        'theory-exam': '理论考试',
        scores: '成绩与排名',
    }[mode];
}
function CompetitionEventWorkspaceModalContent({ route, onNavigate, }) {
    if (route.mode === 'organizations')
        return _jsx(CompetitionEventOrganizationsPage, { eventId: route.eventId, onNavigate: onNavigate });
    if (route.mode === 'rosters')
        return _jsx(CompetitionRosterImportPage, { eventId: route.eventId, onNavigate: onNavigate });
    if (route.mode === 'theory-exam')
        return _jsx(CompetitionTheoryExamPage, { eventId: route.eventId, onNavigate: onNavigate });
    if (route.mode === 'scores')
        return _jsx(CompetitionScoreRankingPage, { eventId: route.eventId, onNavigate: onNavigate });
    return _jsx(CompetitionNoticePage, { eventId: route.eventId, onNavigate: onNavigate });
}
export function CompetitionEventFormPage({ eventId, onNavigate, inModal = false, onClose, }) {
    const isEdit = Boolean(eventId);
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:event:view');
    const canCreate = permissions.includes('competition:event:create');
    const canUpdate = permissions.includes('competition:event:update');
    const [form] = Form.useForm();
    const publishAfterSave = useRef(false);
    const canPublish = permissions.includes('competition:event:publish');
    const queryClient = useQueryClient();
    const goToEvent = (id) => {
        queryClient.invalidateQueries({ queryKey: ['competition-events'] });
        queryClient.invalidateQueries({ queryKey: ['competition-event', String(id)] });
        onNavigate(`/competition/events/${id}`);
    };
    const persistAndMaybePublish = async (save, savedMessage) => {
        const event = await save();
        if (publishAfterSave.current) {
            try {
                await publishCompetitionEvent(event.id);
                message.success('赛事已发布，选手可激活；请继续配置工种和赛段');
            }
            catch {
                message.warning('基础信息已保存，赛事尚未发布，请在赛事详情中重试发布');
            }
        }
        else {
            message.success(savedMessage);
        }
        return event;
    };
    const eventQuery = useQuery({
        queryKey: ['competition-event', eventId],
        queryFn: () => getCompetitionEvent(eventId),
        enabled: canView && isEdit,
    });
    useEffect(() => {
        if (!eventQuery.data)
            return;
        form.setFieldsValue({
            name: eventQuery.data.name,
            scope: eventQuery.data.scope,
            startAt: dateTimeValue(eventQuery.data.startAt),
            endAt: dateTimeValue(eventQuery.data.endAt),
            registrationAuditRequired: eventQuery.data.registrationAuditRequired,
            description: eventQuery.data.description || undefined,
        });
    }, [eventQuery.data, form]);
    const createMutation = useMutation({
        mutationFn: (payload) => persistAndMaybePublish(() => createCompetitionEvent(payload), '赛事已创建，当前状态为创建中'),
        onSuccess: (event) => goToEvent(event.id),
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => persistAndMaybePublish(() => updateCompetitionEvent(id, payload), '赛事基础信息已保存'),
        onSuccess: (event) => {
            queryClient.setQueryData(['competition-event', event.id], event);
            goToEvent(event.id);
        },
    });
    if (!canView || (isEdit && eventQuery.isLoading)) {
        return canView ? _jsx(Spin, { className: "competition-loading" }) : _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u4E8B\u67E5\u770B\u6743\u9650" });
    }
    if (eventQuery.isError) {
        return _jsx(Result, { status: "404", title: "\u8D5B\u4E8B\u4E0D\u5B58\u5728\u6216\u65E0\u6CD5\u8BBF\u95EE", extra: _jsx(Button, { onClick: () => onNavigate('/competition/events'), children: "\u8FD4\u56DE\u5217\u8868" }) });
    }
    const eventStatus = eventQuery.data?.status;
    const canEditBasicInfo = !isEdit
        || eventStatus === 'draft'
        || (eventStatus === 'published' && eventQuery.data?.stagePlanStatus !== 'confirmed');
    if (isEdit && eventQuery.data && !canEditBasicInfo) {
        return (_jsx(Result, { status: "warning", title: "\u5F53\u524D\u8D5B\u4E8B\u4E0D\u53EF\u7F16\u8F91\u57FA\u7840\u4FE1\u606F", subTitle: "\u5DF2\u786E\u8BA4\u8D5B\u6BB5\u7F16\u6392\u3001\u5DF2\u5173\u95ED\u6216\u5DF2\u5F52\u6863\u7684\u8D5B\u4E8B\u4E0D\u80FD\u518D\u4FEE\u6539\u57FA\u7840\u4FE1\u606F\u3002\u53EF\u8FD4\u56DE\u8BE6\u60C5\u7EE7\u7EED\u67E5\u770B\u3002", extra: _jsx(Button, { onClick: () => onNavigate(`/competition/events/${eventId}`), children: "\u67E5\u770B\u8BE6\u60C5" }) }));
    }
    if (!isEdit && !canCreate) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u4E8B\u521B\u5EFA\u6743\u9650" });
    }
    if (isEdit && !canUpdate) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u4E8B\u7F16\u8F91\u6743\u9650" });
    }
    const handleFinish = (values) => {
        if (values.startAt && values.endAt && values.endAt.isBefore(values.startAt)) {
            message.error('结束时间不能早于开始时间');
            return;
        }
        if (isEdit) {
            const payload = {
                name: values.name.trim(),
                startAt: toDateTime(values.startAt),
                endAt: toDateTime(values.endAt),
                registrationAuditRequired: values.registrationAuditRequired,
                description: values.description?.trim() || undefined,
            };
            updateMutation.mutate({ id: eventId, payload });
            return;
        }
        const payload = {
            name: values.name.trim(),
            scope: values.scope,
            startAt: toDateTime(values.startAt),
            endAt: toDateTime(values.endAt),
            registrationAuditRequired: values.registrationAuditRequired,
            description: values.description?.trim() || undefined,
        };
        createMutation.mutate(payload);
    };
    const formContent = (_jsx(_Fragment, { children: _jsxs(Card, { bordered: false, className: "competition-form-card", children: [_jsxs("div", { className: "competition-form-card__intro", children: [_jsx("div", { className: "competition-section-icon", children: _jsx(FileTextOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u586B\u5199\u7ADE\u8D5B\u8BA1\u5212\u57FA\u7840\u4FE1\u606F" }), _jsx(Typography.Text, { type: "secondary", children: "\u57FA\u7840\u4FE1\u606F\u586B\u5199\u5B8C\u6210\u540E\u5373\u53EF\u53D1\u5E03\uFF0C\u9009\u624B\u6FC0\u6D3B\u540E\u7EE7\u7EED\u5B8C\u6210\u8D5B\u6BB5\u4E0E\u9009\u624B\u914D\u7F6E\u3002" })] })] }), _jsxs(Form, { form: form, layout: "vertical", initialValues: { scope: 'platform', registrationAuditRequired: 0 }, onFinish: handleFinish, requiredMark: "optional", children: [_jsxs("div", { className: "competition-form-grid", children: [_jsx(Form.Item, { name: "name", label: "\u8D5B\u4E8B\u540D\u79F0", rules: [{ required: true, whitespace: true, message: '请输入赛事名称' }, { max: 128, message: '名称不能超过128个字符' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8D5B\u4E8B\u540D\u79F0", maxLength: 128, showCount: true }) }), _jsx(Form.Item, { label: "\u8D5B\u4E8B\u7F16\u7801", children: _jsx(Typography.Text, { type: "secondary", children: isEdit ? (eventQuery.data?.code || '保存后由系统生成') : '保存后按赛事名称首拼音 + 创建时间（年月日时）自动生成' }) }), _jsx(Form.Item, { name: "scope", label: "\u8D5B\u4E8B\u8303\u56F4", rules: [{ required: true, message: '请选择赛事范围' }], children: _jsx(Select, { options: scopeOptions, disabled: isEdit }) }), _jsx(Form.Item, { name: "registrationAuditRequired", label: "\u62A5\u540D\u8D44\u683C\u5BA1\u6838", children: _jsx(Select, { options: [{ label: '不需要审核', value: 0 }, { label: '需要审核', value: 1 }] }) }), _jsx(Form.Item, { name: "startAt", label: "\u5F00\u59CB\u65F6\u95F4", children: _jsx(DatePicker, { showTime: true, style: { width: '100%' }, placeholder: "\u8BF7\u9009\u62E9\u5F00\u59CB\u65F6\u95F4" }) }), _jsx(Form.Item, { name: "endAt", label: "\u7ED3\u675F\u65F6\u95F4", dependencies: ['startAt'], rules: [({ getFieldValue }) => ({
                                            validator(_, value) {
                                                const startAt = getFieldValue('startAt');
                                                if (!value || !startAt || !value.isBefore(startAt))
                                                    return Promise.resolve();
                                                return Promise.reject(new Error('结束时间不能早于开始时间'));
                                            },
                                        })], children: _jsx(DatePicker, { showTime: true, style: { width: '100%' }, placeholder: "\u8BF7\u9009\u62E9\u7ED3\u675F\u65F6\u95F4" }) })] }), _jsx(Form.Item, { name: "description", label: "\u8D5B\u4E8B\u63CF\u8FF0", rules: [{ max: 2000, message: '描述不能超过2000个字符' }], children: _jsx(Input.TextArea, { rows: 5, maxLength: 2000, showCount: true, placeholder: "\u8BF7\u8F93\u5165\u8D5B\u4E8B\u63CF\u8FF0" }) }), _jsxs(Space, { children: [_jsx(Button, { onClick: () => (inModal ? onClose?.() : onNavigate('/competition/events')), children: "\u53D6\u6D88" }), _jsx(Button, { htmlType: "submit", loading: createMutation.isPending || updateMutation.isPending, onClick: () => { publishAfterSave.current = false; }, children: "\u4FDD\u5B58\u8D5B\u4E8B\u4FE1\u606F" }), canPublish && eventStatus !== 'published' && (_jsx(Button, { type: "primary", htmlType: "submit", loading: createMutation.isPending || updateMutation.isPending, onClick: () => { publishAfterSave.current = true; }, children: "\u4FDD\u5B58\u5E76\u53D1\u5E03\u8D5B\u4E8B" }))] })] })] }) }));
    if (inModal) {
        return _jsx("div", { className: "competition-form-modal-content", children: formContent });
    }
    return (_jsxs("div", { className: "competition-page", children: [_jsx("section", { className: "competition-page-heading competition-form-hero", children: _jsxs("div", { children: [_jsx(Button, { size: "large", icon: _jsx(ArrowLeftOutlined, {}), onClick: () => onNavigate('/competition/events'), children: "\u8FD4\u56DE" }), _jsx(Typography.Title, { level: 2, children: isEdit ? '编辑竞赛计划' : '创建竞赛计划' })] }) }), formContent] }));
}
export function CompetitionEventPublishButton({ eventId }) {
    const queryClient = useQueryClient();
    const mutation = useMutation({
        mutationFn: () => publishCompetitionEvent(eventId),
        onSuccess: () => {
            message.success('赛事已发布，选手可通过赛事入口激活');
            queryClient.invalidateQueries({ queryKey: ['competition-event', eventId] });
            queryClient.invalidateQueries({ queryKey: ['competition-events'] });
        },
        onError: (error) => message.error(error instanceof Error ? error.message : '赛事发布失败，请重试'),
    });
    return _jsx(Button, { type: "primary", loading: mutation.isPending, onClick: () => Modal.confirm({
            title: '发布赛事？',
            content: '发布后将生成选手激活入口，工种、赛段和选手配置可继续完善。',
            okText: '发布赛事',
            cancelText: '取消',
            onOk: () => mutation.mutateAsync(),
        }), children: "\u53D1\u5E03\u8D5B\u4E8B" });
}
function CompetitionEventDetailPage({ eventId, onNavigate, inModal = false, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:event:view');
    const canUpdate = permissions.includes('competition:event:update');
    const eventQuery = useQuery({
        queryKey: ['competition-event', eventId],
        queryFn: () => getCompetitionEvent(eventId),
        enabled: canView,
    });
    const entryCode = eventQuery.data?.playerEntryCode || '';
    const [entryQrDataUrl, setEntryQrDataUrl] = useState('');
    useEffect(() => {
        let disposed = false;
        if (!entryCode) {
            setEntryQrDataUrl('');
            return () => { disposed = true; };
        }
        QRCode.toDataURL(playerEntryQrPayload(entryCode), {
            width: 360,
            margin: 2,
            errorCorrectionLevel: 'M',
        }).then((dataUrl) => {
            if (!disposed)
                setEntryQrDataUrl(dataUrl);
        }).catch(() => {
            if (!disposed)
                setEntryQrDataUrl('');
        });
        return () => { disposed = true; };
    }, [entryCode]);
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u4E8B\u67E5\u770B\u6743\u9650" });
    if (eventQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (eventQuery.isError || !eventQuery.data) {
        return _jsx(Result, { status: "404", title: "\u8D5B\u4E8B\u4E0D\u5B58\u5728\u6216\u65E0\u6CD5\u8BBF\u95EE", extra: _jsx(Button, { onClick: () => onNavigate('/competition/events'), children: "\u8FD4\u56DE\u5217\u8868" }) });
    }
    const event = eventQuery.data;
    const isDraft = event.status === 'draft';
    const canEditPublished = event.status === 'published' && event.stagePlanStatus !== 'confirmed';
    const workspaceActions = (_jsxs(Space, { wrap: true, children: [isDraft && permissions.includes('competition:event:publish') && _jsx(CompetitionEventPublishButton, { eventId: eventId }), (isDraft || canEditPublished) && canUpdate && (_jsx(Button, { icon: _jsx(EditOutlined, {}), onClick: () => onNavigate(`/competition/events/${event.id}/edit`), children: "\u7F16\u8F91\u8BA1\u5212" }))] }));
    const detailContent = (_jsxs(CompetitionEventFrame, { event: event, activeSection: "overview", onNavigate: onNavigate, actions: workspaceActions, children: [_jsxs("section", { className: "competition-detail-grid competition-detail-grid--single", children: [_jsxs(Card, { bordered: false, className: "competition-detail-card competition-detail-card--main", children: [_jsxs("div", { className: "competition-card-heading", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u7ADE\u8D5B\u8BA1\u5212" }), _jsx(Typography.Text, { type: "secondary", children: "\u57FA\u7840\u4FE1\u606F\u4E0E\u5F53\u524D\u7248\u672C" })] }), _jsx(Tag, { className: "competition-status-tag", color: isDraft ? 'orange' : 'blue', children: isDraft ? '创建中' : '运营中' })] }), _jsxs(Descriptions, { column: { xs: 1, sm: 2 }, children: [_jsx(Descriptions.Item, { label: "\u8BA1\u5212\u540D\u79F0", children: event.name }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u7F16\u7801", children: event.code }), event.status === 'published' && (_jsx(Descriptions.Item, { label: "\u9009\u624B\u7AEF\u8D5B\u4E8B\u5165\u53E3\u7801", span: 2, children: event.playerEntryCode ? (_jsxs(Space, { size: 8, wrap: true, children: [_jsx(Typography.Text, { code: true, copyable: { text: event.playerEntryCode, tooltips: ['复制入口码', '已复制'] }, children: event.playerEntryCode }), _jsx(Button, { size: "small", icon: _jsx(CopyOutlined, {}), onClick: () => void copyText(event.playerEntryCode, '选手端入口码已复制'), children: "\u590D\u5236\u5165\u53E3\u7801" }), _jsx(Typography.Text, { type: "secondary", children: "\u7528\u4E8E\u6253\u5F00\u9009\u624B\u7AEF H5 \u7684 entry \u53C2\u6570" })] })) : (_jsx(Typography.Text, { type: "warning", children: "\u5165\u53E3\u7801\u672A\u751F\u6210\uFF0C\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u6838\u67E5\u53D1\u5E03\u8BB0\u5F55\u3002" })) })), event.status === 'published' && entryCode && (_jsx(Descriptions.Item, { label: "\u8D5B\u4E8B\u6FC0\u6D3B\u4E8C\u7EF4\u7801", span: 2, children: _jsxs(Space, { size: 16, align: "start", wrap: true, children: [entryQrDataUrl ? (_jsx("img", { className: "competition-entry-qr", src: entryQrDataUrl, alt: `${event.name} 选手端激活二维码` })) : _jsx(Spin, { size: "small" }), _jsxs("div", { className: "competition-entry-qr__actions", children: [_jsx(Typography.Text, { children: "\u8BF7\u5728\u9009\u624B\u7AEF App \u9996\u9875\u70B9\u51FB\u201C\u626B\u4E00\u626B\u201D\u540E\u626B\u63CF" }), _jsx(Typography.Text, { type: "secondary", children: "\u4E8C\u7EF4\u7801\u4E0D\u542B\u9009\u624B\u4E2A\u4EBA\u4FE1\u606F\uFF0C\u4EC5\u6307\u5411\u672C\u8D5B\u4E8B\u6FC0\u6D3B\u5165\u53E3\u3002" }), _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), disabled: !entryQrDataUrl, onClick: () => downloadDataUrl(entryQrDataUrl, `${event.code}-选手端赛事二维码.png`), children: "\u4E0B\u8F7D\u4E8C\u7EF4\u7801" })] })] }) })), _jsx(Descriptions.Item, { label: "\u5F00\u59CB\u65F6\u95F4", children: formatDate(event.startAt) }), _jsx(Descriptions.Item, { label: "\u7ED3\u675F\u65F6\u95F4", children: formatDate(event.endAt) }), _jsx(Descriptions.Item, { label: "\u6240\u5C5E\u7EC4\u7EC7", children: event.ownerOrgId ? idValue(event.ownerOrgId) : '平台' }), _jsx(Descriptions.Item, { label: "\u8D5B\u4E8B ID", children: idValue(event.id) }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u8BF4\u660E", span: 2, children: event.description || '暂未填写计划说明' })] })] }), _jsx(CompetitionEventWorkTypeCard, { eventId: idValue(event.id), canView: permissions.includes('competition:event-work-type:view'), canManage: ['draft', 'published'].includes(event.status) && event.stagePlanStatus !== 'confirmed' && permissions.includes('competition:event-work-type:manage') })] }), _jsxs(Card, { bordered: false, className: "competition-detail-card competition-audit-card", children: [_jsxs("div", { className: "competition-card-heading", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u8BA1\u5212\u7559\u75D5" }), _jsx(Typography.Text, { type: "secondary", children: "\u5173\u952E\u64CD\u4F5C\u4E0E\u7248\u672C\u4FE1\u606F" })] }), _jsx(FileTextOutlined, { className: "competition-card-heading__icon" })] }), _jsxs("div", { className: "competition-audit-line", children: [_jsx("span", { className: "competition-audit-dot" }), _jsxs("div", { children: [_jsxs("strong", { children: ["\u5F53\u524D\u72B6\u6001\uFF1A", statusMeta[event.status].label] }), _jsxs("small", { children: ["\u6700\u540E\u66F4\u65B0\u65F6\u95F4\uFF1A", formatDate(event.updatedAt)] })] })] }), _jsxs("div", { className: "competition-audit-line", children: [_jsx("span", { className: "competition-audit-dot competition-audit-dot--muted" }), _jsxs("div", { children: [_jsx("strong", { children: "\u8BA1\u5212\u5DF2\u521B\u5EFA" }), _jsxs("small", { children: ["\u521B\u5EFA\u65F6\u95F4\uFF1A", formatDate(event.createdAt), " \u00B7 \u521B\u5EFA\u4EBA\uFF1A", event.createdBy ? idValue(event.createdBy) : '系统'] })] })] })] })] }));
    return inModal ? _jsx("div", { className: "competition-detail-modal-content", children: detailContent }) : detailContent;
}
function flattenExamWorkTypes(nodes) {
    return nodes.flatMap((node) => {
        if (node.children?.length) {
            return flattenExamWorkTypes(node.children);
        }
        return [{ id: node.id, label: node.title, title: node.title, status: node.status }];
    });
}
function CompetitionEventWorkTypeCard({ eventId, canView, canManage, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canManageLevels = canManage && permissions.includes('competition:stage:manage');
    const canViewLevels = canView && permissions.includes('competition:stage:view');
    const eventWorkTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', eventId],
        queryFn: () => listCompetitionEventWorkTypes(eventId),
        enabled: canView,
    });
    const sourceWorkTypesQuery = useQuery({
        queryKey: ['exam-work-types-tree'],
        queryFn: treeWorkTypes,
        enabled: canView && canManage,
    });
    const queryClient = useQueryClient();
    const [selectedWorkTypeIds, setSelectedWorkTypeIds] = useState([]);
    useEffect(() => {
        if (eventWorkTypesQuery.data) {
            setSelectedWorkTypeIds(eventWorkTypesQuery.data
                .filter((item) => item.status === 'active')
                .map((item) => idValue(item.workTypeId)));
        }
    }, [eventWorkTypesQuery.data]);
    const sourceOptions = useMemo(() => {
        const options = new Map();
        flattenExamWorkTypes(sourceWorkTypesQuery.data || []).forEach((item) => {
            options.set(String(item.id), {
                value: String(item.id),
                label: item.status === 1 ? item.label : `${item.label}（已停用）`,
                title: item.title,
                disabled: item.status !== 1,
            });
        });
        return Array.from(options.values());
    }, [sourceWorkTypesQuery.data]);
    const options = useMemo(() => {
        const optionMap = new Map(sourceOptions.map((option) => [option.value, option]));
        (eventWorkTypesQuery.data || [])
            .filter((item) => item.status === 'active')
            .forEach((item) => {
            const value = idValue(item.workTypeId);
            if (!optionMap.has(value)) {
                optionMap.set(value, {
                    value,
                    label: `${item.displayName || item.workTypeName || value}（工种已不可用）`,
                    title: item.displayName || item.workTypeName || value,
                    disabled: true,
                });
            }
        });
        return Array.from(optionMap.values());
    }, [eventWorkTypesQuery.data, sourceOptions]);
    const optionById = useMemo(() => new Map(options.map((option) => [option.value, option])), [options]);
    const activeEventWorkTypes = useMemo(() => (eventWorkTypesQuery.data || []).filter((item) => item.status === 'active'), [eventWorkTypesQuery.data]);
    const isDirty = useMemo(() => {
        const selected = [...selectedWorkTypeIds].sort();
        const persisted = activeEventWorkTypes.map((item) => idValue(item.workTypeId)).sort();
        return selected.length !== persisted.length || selected.some((id, index) => id !== persisted[index]);
    }, [activeEventWorkTypes, selectedWorkTypeIds]);
    const saveMutation = useMutation({
        mutationFn: async () => {
            const existingByWorkTypeId = new Map();
            (eventWorkTypesQuery.data || []).forEach((item) => {
                existingByWorkTypeId.set(idValue(item.workTypeId), item);
            });
            for (const [index, workTypeId] of selectedWorkTypeIds.entries()) {
                const option = optionById.get(workTypeId);
                const existing = existingByWorkTypeId.get(workTypeId);
                if (!option || option.disabled)
                    continue;
                const payload = {
                    workTypeId: Number(workTypeId),
                    displayName: existing?.displayName || option.title,
                    status: 'active',
                    sortOrder: index,
                };
                if (!existing) {
                    await createCompetitionEventWorkType(eventId, payload);
                }
                else if (existing.status !== 'active'
                    || existing.sortOrder !== index
                    || existing.displayName !== payload.displayName) {
                    await updateCompetitionEventWorkType(eventId, existing.id, payload);
                }
            }
            for (const existing of eventWorkTypesQuery.data || []) {
                if (existing.status !== 'active' || selectedWorkTypeIds.includes(idValue(existing.workTypeId)))
                    continue;
                await deleteCompetitionEventWorkType(eventId, existing.id);
            }
        },
        onSuccess: () => {
            message.success('参赛工种已保存');
            queryClient.invalidateQueries({ queryKey: ['competition-event-work-types', eventId] });
        },
    });
    return (_jsxs(Card, { bordered: false, className: "competition-detail-card competition-event-work-type-card", children: [_jsx("div", { className: "competition-card-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u53C2\u8D5B\u5DE5\u79CD" }), _jsx(Typography.Text, { type: "secondary", children: "\u4ECE\u8BFE\u7A0B\u7BA1\u7406\u7684\u5DE5\u79CD\u7BA1\u7406\u4E2D\u9009\u62E9\u672C\u8D5B\u4E8B\u53C2\u8D5B\u5DE5\u79CD\uFF0C\u652F\u6301\u591A\u9009" })] }) }), !canView ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u53C2\u8D5B\u5DE5\u79CD\u67E5\u770B\u6743\u9650" })) : eventWorkTypesQuery.isLoading || (canManage && sourceWorkTypesQuery.isLoading) ? (_jsx(Spin, { size: "small" })) : eventWorkTypesQuery.isError ? (_jsx(Typography.Text, { type: "danger", children: "\u53C2\u8D5B\u5DE5\u79CD\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u5237\u65B0\u91CD\u8BD5" })) : canManage ? (_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Select, { mode: "multiple", allowClear: true, showSearch: true, optionFilterProp: "label", style: { width: '100%' }, placeholder: "\u8BF7\u9009\u62E9\u53C2\u8D5B\u5DE5\u79CD\uFF08\u652F\u6301\u591A\u9009\uFF09", value: selectedWorkTypeIds, options: options, loading: sourceWorkTypesQuery.isFetching, onChange: (values) => setSelectedWorkTypeIds(values.map(String)) }), _jsxs(Space, { children: [_jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), loading: saveMutation.isPending, disabled: !isDirty || sourceWorkTypesQuery.isError, onClick: () => saveMutation.mutate(), children: "\u4FDD\u5B58\u53C2\u8D5B\u5DE5\u79CD" }), _jsx(Typography.Text, { type: "secondary", children: "\u8D5B\u4E8B\u7ED1\u5B9A\u53EA\u5F71\u54CD\u672C\u8D5B\u4E8B\uFF0C\u4E0D\u4F1A\u4FEE\u6539\u8BFE\u7A0B\u7BA1\u7406\u4E2D\u7684\u5DE5\u79CD" })] })] })) : (_jsx(Space, { wrap: true, children: activeEventWorkTypes.length ? activeEventWorkTypes.map((item) => (_jsx(Tag, { children: item.displayName || item.workTypeName || idValue(item.workTypeId) }, item.id))) : _jsx(Typography.Text, { type: "secondary", children: "\u6682\u672A\u8BBE\u7F6E\u53C2\u8D5B\u5DE5\u79CD" }) })), canViewLevels && activeEventWorkTypes.map((workType) => (_jsx(CompetitionEventWorkTypeLevelEditor, { eventId: eventId, workType: workType, canManage: canManageLevels }, `levels-${workType.id}`)))] }));
}
function CompetitionEventWorkTypeLevelEditor({ eventId, workType, canManage, }) {
    const queryClient = useQueryClient();
    const levelsQuery = useQuery({
        queryKey: ['competition-event-work-type-levels', eventId, workType.id],
        queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workType.id),
        enabled: true,
    });
    const [selectedCodes, setSelectedCodes] = useState([]);
    useEffect(() => {
        if (levelsQuery.data) {
            setSelectedCodes(levelsQuery.data
                .map((level) => level.levelCode)
                .filter((code) => Boolean(code)));
        }
    }, [levelsQuery.data]);
    const persistedCodes = useMemo(() => (levelsQuery.data || []).map((level) => level.levelCode).sort(), [levelsQuery.data]);
    const isDirty = useMemo(() => {
        const selected = [...selectedCodes].sort();
        return selected.length !== persistedCodes.length
            || selected.some((code, index) => code !== persistedCodes[index]);
    }, [persistedCodes, selectedCodes]);
    const saveMutation = useMutation({
        mutationFn: () => {
            const existingByCode = new Map((levelsQuery.data || []).map((level) => [level.levelCode, level]));
            const payload = selectedCodes.map((code) => {
                const existing = existingByCode.get(code);
                const option = competitionLevelOptions.find((item) => item.value === code);
                return {
                    id: existing?.id,
                    levelCode: code,
                    levelName: existing?.levelName || option?.label || code,
                    sourceLevelCode: existing?.sourceLevelCode,
                    sourceLevelText: existing?.sourceLevelText,
                    reconciliationStatus: existing?.reconciliationStatus || 'matched',
                };
            });
            return replaceCompetitionEventWorkTypeLevels(eventId, workType.id, payload);
        },
        onSuccess: (levels) => {
            message.success(`“${workType.displayName || workType.workTypeName || idValue(workType.workTypeId)}”参赛等级已保存`);
            queryClient.setQueryData(['competition-event-work-type-levels', eventId, workType.id], levels);
            queryClient.invalidateQueries({ queryKey: ['competition-event-publish-readiness', eventId] });
        },
        onError: (error) => message.error(error instanceof Error ? error.message : '参赛等级保存失败，请刷新重试'),
    });
    if (levelsQuery.isLoading) {
        return _jsx(Spin, { size: "small" });
    }
    if (levelsQuery.isError) {
        return _jsx(Typography.Text, { type: "danger", children: "\u7B49\u7EA7\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u5237\u65B0\u91CD\u8BD5" });
    }
    const title = workType.displayName || workType.workTypeName || idValue(workType.workTypeId);
    return (_jsx(Card, { size: "small", type: "inner", title: `“${title}”参赛等级`, style: { width: '100%' }, children: canManage ? (_jsxs(Space, { direction: "vertical", size: 8, style: { width: '100%' }, children: [_jsx(Select, { mode: "multiple", allowClear: true, style: { width: '100%' }, placeholder: "\u8BF7\u9009\u62E9\u8BE5\u5DE5\u79CD\u7684\u53C2\u8D5B\u7B49\u7EA7\uFF08\u81F3\u5C11\u4E00\u9879\uFF09", options: competitionLevelOptions, value: selectedCodes, onChange: (values) => setSelectedCodes(values.map(String)) }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", size: "small", icon: _jsx(CheckCircleOutlined, {}), loading: saveMutation.isPending, disabled: !isDirty, onClick: () => saveMutation.mutate(), children: "\u4FDD\u5B58\u53C2\u8D5B\u7B49\u7EA7" }), _jsx(Typography.Text, { type: "secondary", children: "\u65B0\u589E\u5DE5\u79CD\u4E0D\u4F1A\u9ED8\u8BA4\u9009\u62E9\u7B49\u7EA7\uFF1B\u8D5B\u4E8B\u53D1\u5E03\u540E\u53EF\u7EE7\u7EED\u914D\u7F6E\uFF0C\u786E\u8BA4\u8D5B\u6BB5\u7F16\u6392\u524D\u8BF7\u9009\u62E9\u53C2\u8D5B\u7B49\u7EA7\u3002" })] })] })) : (_jsx(Space, { wrap: true, children: (levelsQuery.data || []).length ? (levelsQuery.data || []).map((level) => (_jsxs(Tag, { color: level.stagePlanStatus === 'confirmed' ? 'success' : 'default', children: [level.levelName || level.levelCode, level.stagePlanStatus === 'confirmed' ? ' · 已确认' : ' · 待确认'] }, level.id))) : _jsx(Typography.Text, { type: "warning", children: "\u672A\u660E\u786E\u9009\u62E9\u53C2\u8D5B\u7B49\u7EA7" }) })) }));
}
