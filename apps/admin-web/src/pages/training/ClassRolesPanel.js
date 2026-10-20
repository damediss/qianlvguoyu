import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 班级角色维护（train_class_role）
 * 开班准备弹窗与跟班工作台共用，数据同一真相源。
 */
import { useDeferredValue, useState } from 'react';
import { Alert, Button, Form, Input, Modal, Select, Space, Table, Typography, message, } from 'antd';
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
const ROLE_FILTER_OPTIONS = [
    { value: 'all', label: '全部角色' },
    { value: 'follow_staff', label: '教务人员' },
    { value: 'lead_teacher', label: '理论老师' },
    { value: 'assistant', label: '实操老师' },
    { value: 'monitor', label: '班长' },
];
const ASSIGN_ROLE_OPTIONS = [
    { value: 'lead_teacher', label: '理论老师' },
    { value: 'assistant', label: '实操老师' },
    { value: 'follow_staff', label: '教务人员' },
];
function matchesRoleFilter(roleCode, filter) {
    if (filter === 'all')
        return true;
    if (filter === 'follow_staff')
        return roleCode === 'follow_staff' || roleCode === 'head_teacher';
    return roleCode === filter;
}
function invalidateRoleQueries(qc, classId) {
    void qc.invalidateQueries({ queryKey: ['class-roles', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-role-summary', classId] });
    void qc.invalidateQueries({ queryKey: ['class-role-summary', classId] });
}
/** 内嵌面板：列表 + 指派/卸任（与开班准备同源 API） */
export function ClassRolesPanel({ classId, showAlert = true, contextHint }) {
    const qc = useQueryClient();
    const [roleFilter, setRoleFilter] = useState('all');
    const [importOpen, setImportOpen] = useState(false);
    const [assignRoleCode, setAssignRoleCode] = useState('lead_teacher');
    const [selectedTeacherIds, setSelectedTeacherIds] = useState([]);
    const [importPage, setImportPage] = useState(1);
    const [importPageSize, setImportPageSize] = useState(8);
    const [teacherKeyword, setTeacherKeyword] = useState('');
    const deferredTeacherKeyword = useDeferredValue(teacherKeyword.trim());
    const { data: roles = [], isLoading, refetch, isFetching } = useQuery({
        queryKey: ['class-roles', classId],
        queryFn: () => trainApi.listClassRoles(classId),
        enabled: !!classId,
    });
    const { data: teacherPage, isFetching: teachersLoading } = useQuery({
        queryKey: ['class-roles-teachers', importPage, importPageSize, deferredTeacherKeyword],
        queryFn: () => trainApi.pageTeacherBanks({
            page: importPage,
            pageSize: importPageSize,
            keyword: deferredTeacherKeyword || undefined,
        }),
        enabled: importOpen,
    });
    const assignMu = useMutation({
        mutationFn: async (payload) => {
            for (const id of payload.ids) {
                await trainApi.assignClassRole(classId, {
                    roleCode: payload.roleCode,
                    subjectType: payload.subjectType,
                    subjectId: id,
                });
            }
        },
        onSuccess: () => {
            message.success('指派成功（开班准备与跟班已同步）');
            setImportOpen(false);
            setSelectedTeacherIds([]);
            invalidateRoleQueries(qc, classId);
        },
        onError: (e) => message.error(e.message || '指派失败'),
    });
    const endMu = useMutation({
        mutationFn: (roleId) => trainApi.endClassRole(classId, roleId),
        onSuccess: () => {
            message.success('已卸任（开班准备与跟班已同步）');
            invalidateRoleQueries(qc, classId);
        },
        onError: (e) => message.error(e.message || '卸任失败'),
    });
    const filtered = roles.filter((r) => matchesRoleFilter(r.roleCode, roleFilter));
    const unboundCount = roles.filter((r) => r.unboundWarning).length;
    const columns = [
        { title: '角色', dataIndex: 'roleName', width: 120 },
        { title: '姓名', dataIndex: 'displayName', ellipsis: true },
        { title: '手机', dataIndex: 'phone', width: 130, render: (v) => v || '—' },
        {
            title: 'App账号',
            width: 100,
            align: 'center',
            render: (_, row) => row.unboundWarning ? (_jsx(Typography.Text, { type: "warning", children: "\u672A\u7ED1\u5B9A" })) : (_jsx(Typography.Text, { type: "success", children: "\u5DF2\u7ED1\u5B9A" })),
        },
        {
            title: '操作',
            width: 100,
            align: 'center',
            render: (_, row) => row.roleCode === 'monitor' ? (_jsx(Typography.Text, { type: "secondary", children: "\u82B1\u540D\u518C" })) : (_jsx(Button, { type: "link", danger: true, size: "small", loading: endMu.isPending, onClick: () => Modal.confirm({
                    title: '确认卸任？',
                    content: `将解除「${row.roleName}」：${row.displayName || '—'}`,
                    onOk: () => endMu.mutateAsync(row.id),
                }), children: "\u5378\u4EFB" })),
        },
    ];
    return (_jsxs("div", { children: [showAlert && (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u73ED\u7EA7\u89D2\u8272\u4E0E\u5F00\u73ED\u51C6\u5907\u5171\u7528 train_class_role\uFF0C\u4E24\u5904\u7EF4\u62A4\u5B9E\u65F6\u540C\u6B65", description: _jsxs(_Fragment, { children: [unboundCount > 0
                            ? `有 ${unboundCount} 人未绑定 App 账号，指派后需确保可登录。`
                            : '可从师资库指派理论老师、实操老师、教务人员。', ' ', "\u73ED\u957F\u8BF7\u5728\u5F00\u73ED\u51C6\u5907\u300C\u5B66\u5458\u5217\u8868\u300D\u8BBE\u7F6E\u662F\u5426\u73ED\u957F\uFF08\u7CFB\u7EDF\u81EA\u52A8\u540C\u6B65\uFF09\u3002", contextHint ? ` ${contextHint}` : ''] }) })), _jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsx(Select, { value: roleFilter, style: { width: 160 }, onChange: setRoleFilter, options: ROLE_FILTER_OPTIONS }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                            setAssignRoleCode('lead_teacher');
                            setTeacherKeyword('');
                            setImportPage(1);
                            setSelectedTeacherIds([]);
                            setImportOpen(true);
                        }, children: "\u4ECE\u5E08\u8D44\u5E93\u6307\u6D3E" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: isFetching, onClick: () => void refetch(), children: "\u5237\u65B0" }), _jsx(Typography.Text, { type: "secondary", children: "\u73ED\u957F\u8BF7\u5728\u5B66\u5458\u5217\u8868\u8BBE\u7F6E" })] }), _jsx(Table, { rowKey: "id", loading: isLoading, dataSource: filtered, columns: columns, pagination: false, locale: { emptyText: '暂无角色，请从师资库指派或设置班长' }, size: "small" }), _jsxs(Modal, { title: "\u4ECE\u5E08\u8D44\u5E93\u6307\u6D3E", open: importOpen, onCancel: () => setImportOpen(false), onOk: () => {
                    if (!selectedTeacherIds.length) {
                        message.warning('请选择人员');
                        return;
                    }
                    return assignMu.mutateAsync({
                        roleCode: assignRoleCode,
                        subjectType: 'teacher_bank',
                        ids: selectedTeacherIds,
                    });
                }, confirmLoading: assignMu.isPending, width: 720, destroyOnHidden: true, children: [_jsx(Form.Item, { label: "\u89D2\u8272", children: _jsx(Select, { value: assignRoleCode, onChange: setAssignRoleCode, options: ASSIGN_ROLE_OPTIONS }) }), _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D / \u624B\u673A / \u8EAB\u4EFD\u8BC1", value: teacherKeyword, onChange: (e) => {
                            setTeacherKeyword(e.target.value);
                            setImportPage(1);
                        }, style: { marginBottom: 12 } }), _jsx(Table, { rowKey: "id", size: "small", loading: teachersLoading, dataSource: teacherPage?.items ?? [], columns: [
                            { title: '姓名', dataIndex: 'name', width: 100 },
                            {
                                title: '身份证号',
                                width: 170,
                                render: (_, row) => {
                                    const v = row.idCardNumber?.trim();
                                    if (v && (/^[0-9Xx]{15}$/.test(v) || /^[0-9Xx]{18}$/.test(v)))
                                        return v;
                                    return '—';
                                },
                            },
                            { title: '手机', dataIndex: 'phone', width: 130, render: (v) => v || '—' },
                        ], rowSelection: {
                            selectedRowKeys: selectedTeacherIds,
                            preserveSelectedRowKeys: true,
                            onChange: (keys) => setSelectedTeacherIds(keys),
                        }, pagination: {
                            current: importPage,
                            pageSize: importPageSize,
                            total: teacherPage?.total ?? 0,
                            showSizeChanger: true,
                            pageSizeOptions: ['8', '20', '50', '100'],
                            showTotal: (t) => `共 ${t} 人`,
                            onChange: (p, size) => {
                                if (typeof size === 'number' && size > 0 && size !== importPageSize) {
                                    setImportPageSize(size);
                                    setImportPage(1);
                                }
                                else {
                                    setImportPage(p);
                                }
                            },
                        } })] })] }));
}
/** 开班准备用弹窗壳 */
export function ClassRolesModal({ open, onClose, classId, }) {
    return (_jsx(Modal, { title: "\u73ED\u7EA7\u89D2\u8272", open: open, onCancel: onClose, footer: null, width: "64%", destroyOnHidden: true, children: classId != null ? (_jsx(ClassRolesPanel, { classId: classId })) : (_jsx(Typography.Text, { type: "secondary", children: "\u672A\u9009\u62E9\u73ED\u7EA7" })) }));
}
