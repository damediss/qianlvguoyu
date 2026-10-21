import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, ReloadOutlined, TeamOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { addPreparationGroupMember, createPreparationAssessorGroup, deletePreparationAssessorGroup, listPreparationAssessorGroupWorktypes, listPreparationAssessorGroups, listPreparationGroupMembers, removePreparationGroupMember, updatePreparationAssessorGroup, } from '../../../api/certificationPreparation';
import { listAvailableAssessors } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import { dash, dateText, statusTag } from './shared';
export function AssessorConfig({ taskId, task, onChanged }) {
    const queryClient = useQueryClient();
    const [selectedGroupId, setSelectedGroupId] = useState();
    const [groupModalOpen, setGroupModalOpen] = useState(false);
    const [memberWizardOpen, setMemberWizardOpen] = useState(false);
    const [candidateKeyword, setCandidateKeyword] = useState('');
    const [selectedCandidateIds, setSelectedCandidateIds] = useState([]);
    const [editingGroup, setEditingGroup] = useState(null);
    const [groupForm] = Form.useForm();
    const groupsQuery = useQuery({
        queryKey: ['certification-preparation-assessor-groups', taskId],
        queryFn: () => listPreparationAssessorGroups(taskId),
        enabled: Boolean(taskId),
    });
    const membersQuery = useQuery({
        queryKey: ['certification-preparation-assessor-members', taskId, selectedGroupId],
        queryFn: () => listPreparationGroupMembers(taskId, selectedGroupId),
        enabled: Boolean(taskId
            && selectedGroupId
            && (groupsQuery.data || []).some((group) => group.id === selectedGroupId)),
    });
    const editingGroupMembersQuery = useQuery({
        queryKey: ['certification-preparation-assessor-members-for-edit', taskId, editingGroup?.id],
        queryFn: () => listPreparationGroupMembers(taskId, editingGroup.id),
        enabled: Boolean(taskId && editingGroup?.id),
    });
    const taskWorktypesQuery = useQuery({
        queryKey: ['certification-preparation-task-worktypes', taskId],
        queryFn: () => listPreparationAssessorGroupWorktypes(taskId),
        enabled: Boolean(taskId),
    });
    useEffect(() => {
        const groups = groupsQuery.data || [];
        if (!selectedGroupId && groups.length > 0) {
            setSelectedGroupId(groups[0].id);
        }
        if (selectedGroupId && groups.length > 0 && !groups.some((group) => group.id === selectedGroupId)) {
            setSelectedGroupId(groups[0].id);
        }
        if (selectedGroupId && !groupsQuery.isFetching && groups.length === 0) {
            setSelectedGroupId(undefined);
        }
    }, [groupsQuery.data, selectedGroupId]);
    const selectedGroup = (groupsQuery.data || []).find((group) => group.id === selectedGroupId);
    const requiredWorkTypeScopes = selectedGroup?.workTypeScopes?.length
        ? selectedGroup.workTypeScopes
        : taskWorktypesQuery.data || [];
    const allRequiredWorkTypesHaveIds = requiredWorkTypeScopes.length > 0
        && requiredWorkTypeScopes.every((scope) => Boolean(scope.workTypeId));
    const requiredWorkTypeIds = allRequiredWorkTypesHaveIds
        ? [...new Set(requiredWorkTypeScopes.map((scope) => String(scope.workTypeId)))]
        : [];
    const requiredWorkTypeNames = allRequiredWorkTypesHaveIds
        ? []
        : [...new Set(requiredWorkTypeScopes.map((scope) => scope.workTypeName || '').filter(Boolean))];
    const availableCandidatesQuery = useQuery({
        queryKey: [
            'certification-info-assessors-available', candidateKeyword, requiredWorkTypeIds, requiredWorkTypeNames,
        ],
        queryFn: () => listAvailableAssessors(candidateKeyword || undefined, true, requiredWorkTypeIds, requiredWorkTypeNames, true),
        enabled: memberWizardOpen,
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-groups', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-members', taskId, selectedGroupId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-info-assessors-available'] });
        queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', taskId] });
        onChanged?.();
    };
    const createGroupMutation = useMutation({
        mutationFn: (values) => createPreparationAssessorGroup(taskId, values),
        onSuccess: (group) => {
            message.success('考评组已创建');
            setSelectedGroupId(group.id);
            setGroupModalOpen(false);
            groupForm.resetFields();
            refresh();
        },
    });
    const updateGroupMutation = useMutation({
        mutationFn: ({ id, values }) => updatePreparationAssessorGroup(taskId, id, values),
        onSuccess: () => {
            message.success('考评组已更新');
            setGroupModalOpen(false);
            setEditingGroup(null);
            groupForm.resetFields();
            refresh();
        },
    });
    const deleteGroupMutation = useMutation({
        mutationFn: (groupId) => deletePreparationAssessorGroup(taskId, groupId),
        onSuccess: () => {
            message.success('考评组已删除');
            refresh();
        },
    });
    const addMembersMutation = useMutation({
        mutationFn: async () => {
            if (!taskId || !selectedGroupId)
                throw new Error('请先选择考评组');
            if (selectedCandidateIds.length === 0) {
                throw new Error('请至少选择 1 名成员');
            }
            const members = [];
            for (const assessorId of selectedCandidateIds) {
                members.push(await addPreparationGroupMember(taskId, selectedGroupId, {
                    assessorId,
                    taskRole: 'member',
                    roleName: '成员',
                }));
            }
            return members;
        },
        onSuccess: (members) => {
            message.success(`已添加 ${members.length} 名考评成员`);
            closeMemberWizard();
            refresh();
        },
    });
    const removeMemberMutation = useMutation({
        mutationFn: (memberId) => removePreparationGroupMember(taskId, selectedGroupId, memberId),
        onSuccess: () => {
            message.success('考评组成员已移除');
            refresh();
        },
    });
    const groupColumns = [
        { title: '考评组', dataIndex: 'groupName', minWidth: 180, render: dash },
        {
            title: '工种/科目',
            dataIndex: 'workTypeScopes',
            width: 240,
            render: (scopes) => {
                if (!scopes || scopes.length === 0)
                    return '-';
                return scopes.map((s) => [s.workTypeName, s.levelName].filter(Boolean).join('-')).join('、');
            },
        },
        { title: '组长', dataIndex: 'leaderAssessorName', width: 140, render: dash },
        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
        {
            title: '操作',
            width: 210,
            render: (_, record) => (_jsx(Space, { size: 4, children: _jsxs(PermissionGate, { code: "certification.preparation.assessor.assign", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEditGroup(record), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u8003\u8BC4\u7EC4\uFF1F", description: "\u5220\u9664\u540E\u4E0D\u53EF\u6062\u590D\uFF1B\u82E5\u5B58\u5728\u6210\u5458\u6216\u5DF2\u53D1\u653E\u4EFB\u52A1\u5C06\u65E0\u6CD5\u5220\u9664\u3002", okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => deleteGroupMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] }) })),
        },
    ];
    const memberColumns = [
        { title: '姓名', dataIndex: 'assessorName', width: 120 },
        { title: '证号', dataIndex: 'certificateNo', width: 160, render: dash },
        { title: '任务角色', dataIndex: 'roleName', width: 120, render: (_, row) => row.roleName || roleName(row.taskRole) },
        { title: '工种范围', dataIndex: 'workTypeScope', minWidth: 180, render: dash },
        { title: '有效期至', dataIndex: 'validUntil', width: 120, render: dateText },
        { title: '工作状态', dataIndex: 'workStatus', width: 110, render: statusTag },
        {
            title: '操作',
            width: 110,
            render: (_, record) => (_jsx(PermissionGate, { code: "certification.preparation.assessor.assign", children: _jsx(Popconfirm, { title: "\u786E\u8BA4\u79FB\u9664\u8BE5\u6210\u5458\uFF1F", okText: "\u79FB\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => removeMemberMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u79FB\u9664" }) }) })),
        },
    ];
    function openEditGroup(record) {
        setEditingGroup(record);
        groupForm.setFieldsValue({
            groupName: record.groupName,
            leaderAssessorId: record.leaderAssessorId,
            workTypeScopeIds: (record.workTypeScopes || []).map((s) => String(s.scopeId)),
            remark: record.remark,
        });
        setGroupModalOpen(true);
    }
    function submitGroup() {
        groupForm.validateFields().then((values) => {
            const payload = {
                groupName: values.groupName,
                leaderAssessorId: values.leaderAssessorId,
                workTypeScopeIds: values.workTypeScopeIds,
                remark: values.remark,
            };
            if (editingGroup) {
                updateGroupMutation.mutate({ id: editingGroup.id, values: payload });
                return;
            }
            createGroupMutation.mutate(payload);
        });
    }
    function openMemberWizard() {
        if (!taskId || !selectedGroupId) {
            message.warning('请先选择考评组');
            return;
        }
        setCandidateKeyword('');
        setSelectedCandidateIds([]);
        setMemberWizardOpen(true);
    }
    function closeMemberWizard() {
        setMemberWizardOpen(false);
        setCandidateKeyword('');
        setSelectedCandidateIds([]);
    }
    const candidateColumns = [
        { title: '姓名', dataIndex: 'assessorName', width: 120, render: dash },
        { title: '证号', dataIndex: 'certificateNo', width: 160, render: dash },
        {
            title: '工种范围',
            dataIndex: 'workTypeScopes',
            minWidth: 180,
            render: (value) => (value && value.length > 0 ? value.join('、') : '-'),
        },
        { title: '等级范围', dataIndex: 'levelScope', width: 150, render: dash },
        { title: '有效期至', dataIndex: 'validUntil', width: 120, render: dateText },
        { title: '当前状态', dataIndex: 'workStatus', width: 110, render: statusTag },
    ];
    const worktypeOptions = (taskWorktypesQuery.data || []).map((w) => {
        const label = [w.workTypeName, w.levelName].filter(Boolean).join('-');
        return { label, value: String(w.scopeId) };
    });
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(ProCard, { title: "\u4EFB\u52A1\u8003\u8BC4\u7EC4", extra: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), _jsx(PermissionGate, { code: "certification.preparation.assessor.assign", children: _jsx(Button, { icon: _jsx(TeamOutlined, {}), disabled: !taskId, onClick: () => {
                                    setEditingGroup(null);
                                    groupForm.resetFields();
                                    groupForm.setFieldsValue({ workTypeScopeIds: [] });
                                    setGroupModalOpen(true);
                                }, children: "\u65B0\u5EFA\u8003\u8BC4\u7EC4" }) })] }), children: !taskId ? (_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u914D\u7F6E\u8003\u8BC4\u7EC4\u3002" })) : (_jsx(Table, { rowKey: "id", columns: groupColumns, dataSource: groupsQuery.data || [], loading: groupsQuery.isFetching, pagination: false, rowClassName: (record) => record.id === selectedGroupId ? 'selected-row' : '', onRow: (record) => ({ onClick: () => setSelectedGroupId(record.id) }) })) }), _jsx(ProCard, { title: selectedGroup ? `成员：${selectedGroup.groupName || selectedGroup.id}` : '考评组成员', extra: _jsx(PermissionGate, { code: "certification.preparation.assessor.assign", children: _jsx(Button, { disabled: !taskId || !selectedGroupId, icon: _jsx(TeamOutlined, {}), onClick: openMemberWizard, children: "\u6DFB\u52A0\u6210\u5458" }) }), children: _jsx(Table, { rowKey: "id", columns: memberColumns, dataSource: membersQuery.data || [], loading: membersQuery.isFetching, pagination: false, scroll: { x: 920 } }) }), _jsx(Modal, { title: editingGroup ? '编辑考评组' : '创建考评组', open: groupModalOpen, onOk: submitGroup, onCancel: () => setGroupModalOpen(false), confirmLoading: createGroupMutation.isPending || updateGroupMutation.isPending, width: 680, children: _jsxs(Form, { form: groupForm, layout: "vertical", children: [_jsx(Form.Item, { name: "groupName", label: "\u8003\u8BC4\u7EC4\u540D\u79F0", rules: [{ required: true, message: '请输入考评组名称' }], children: _jsx(Input, {}) }), editingGroup ? (_jsx(Form.Item, { name: "leaderAssessorId", label: "\u7EC4\u957F", rules: [{ required: true, message: '请选择组长' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", options: (editingGroupMembersQuery.data || []).map((m) => ({
                                    label: `${m.assessorName || '-'} ${m.certificateNo || ''}`,
                                    value: m.assessorId,
                                })), loading: editingGroupMembersQuery.isFetching, placeholder: "\u4ECE\u672C\u7EC4\u6210\u5458\u4E2D\u9009\u62E9\u7EC4\u957F" }) })) : (_jsx(Typography.Paragraph, { type: "secondary", children: "\u5148\u521B\u5EFA\u7A7A\u8003\u8BC4\u7EC4\uFF0C\u518D\u5728\u4E0B\u65B9\"\u8003\u8BC4\u7EC4\u6210\u5458\"\u533A\u57DF\u6DFB\u52A0\u6210\u5458\uFF0C\u6700\u540E\u7F16\u8F91\u8003\u8BC4\u7EC4\u6307\u5B9A\u7EC4\u957F\u3002" })), _jsx(Form.Item, { name: "workTypeScopeIds", label: "\u5DE5\u79CD/\u79D1\u76EE", rules: [{ required: true, message: '请选择至少一个工种' }], extra: worktypeOptions.length === 0 ? '当前任务尚未配置工种范围，请先在认定任务中维护' : '可多选，范围严格限定为当前认定任务已配置的工种', children: _jsx(Select, { mode: "multiple", allowClear: true, showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", loading: taskWorktypesQuery.isFetching, options: worktypeOptions, disabled: worktypeOptions.length === 0 }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2 }) })] }) }), _jsx(Modal, { title: `添加成员${selectedGroup ? ` - ${selectedGroup.groupName}` : ''}`, open: memberWizardOpen, onCancel: closeMemberWizard, width: 920, okText: "\u6DFB\u52A0\u5230\u5F53\u524D\u8003\u8BC4\u7EC4", confirmLoading: addMembersMutation.isPending, onOk: () => {
                    if (selectedCandidateIds.length === 0) {
                        message.warning('请至少选择 1 名成员');
                        return;
                    }
                    addMembersMutation.mutate();
                }, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [availableCandidatesQuery.isError ? (_jsx(Alert, { type: "error", showIcon: true, message: "\u53EF\u7528\u8003\u8BC4\u5458\u7B5B\u9009\u5931\u8D25", description: candidateQueryErrorMessage(availableCandidatesQuery.error) })) : null, _jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u59D3\u540D\u3001\u8BC1\u53F7", onSearch: (value) => setCandidateKeyword(value || '') }), _jsx(Table, { rowKey: "id", columns: candidateColumns, dataSource: availableCandidatesQuery.data || [], loading: availableCandidatesQuery.isFetching, pagination: false, scroll: { x: 860, y: 360 }, rowSelection: {
                                selectedRowKeys: selectedCandidateIds,
                                onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
                            } }), _jsxs(Typography.Text, { type: "secondary", children: ["\u5DF2\u9009 ", selectedCandidateIds.length, " \u4EBA\uFF08\u5019\u9009\u4E3A\u5DF2\u5165\u5E93\u3001\u8BC1\u4E66\u672A\u8FC7\u671F\u7684\u8003\u8BC4\u5458\uFF1B\u5DF2\u7ED1\u5B9A\u5176\u4ED6\u4EFB\u52A1\u4ECD\u53EF\u9009\u62E9\uFF09"] })] }) })] }));
}
function roleName(role) {
    return role === 'leader' ? '组长' : role === 'member' ? '成员' : dash(role);
}
function candidateQueryErrorMessage(error) {
    if (error && typeof error === 'object') {
        const responseMessage = error
            .response?.data?.message;
        if (typeof responseMessage === 'string' && responseMessage.trim())
            return responseMessage;
        if (error instanceof Error && error.message)
            return error.message;
    }
    return '请检查认定任务工种范围和考评员历史授权，并修正后重试。';
}
