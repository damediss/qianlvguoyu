import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { bindPreparationTaskEquipment, listPreparationTaskEquipment, removePreparationTaskEquipment, } from '../../../api/certificationPreparation';
import { listAvailableEquipment } from '../../../api/certificationInfo';
import { FilePreviewModal } from '../../../components/FilePreviewModal';
import { PermissionGuard } from '../../../directives/permission';
import { dash, statusTag } from './shared';
export function EquipmentConfig({ taskId, onChanged }) {
    const queryClient = useQueryClient();
    const [assignModalOpen, setAssignModalOpen] = useState(false);
    const [previewTarget, setPreviewTarget] = useState(null);
    const [assignForm] = Form.useForm();
    const availableEquipmentQuery = useQuery({
        queryKey: ['certification-info-equipment-available'],
        queryFn: () => listAvailableEquipment(),
        enabled: assignModalOpen,
    });
    const taskEquipmentQuery = useQuery({
        queryKey: ['certification-preparation-task-equipment', taskId],
        queryFn: () => listPreparationTaskEquipment(taskId),
        enabled: Boolean(taskId),
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-equipment', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-info-equipment-available'] });
        queryClient.invalidateQueries({ queryKey: ['certification-info-equipment'] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        onChanged?.();
    };
    const assignMutation = useMutation({
        mutationFn: (values) => bindPreparationTaskEquipment(taskId, {
            equipmentId: values.equipmentId,
            remark: values.remark,
        }),
        onSuccess: () => {
            message.success('设备已绑定到任务');
            setAssignModalOpen(false);
            assignForm.resetFields();
            refresh();
        },
    });
    const removeMutation = useMutation({
        mutationFn: (taskEquipmentId) => removePreparationTaskEquipment(taskId, taskEquipmentId),
        onSuccess: () => {
            message.success('任务设备已删除');
            refresh();
        },
    });
    const columns = [
        { title: '设备名', dataIndex: 'equipmentName', width: 180 },
        { title: '设备编号', dataIndex: 'equipmentNo', width: 160 },
        { title: '设备数量', dataIndex: 'equipmentQuantity', width: 100 },
        { title: '备注', dataIndex: 'remark', ellipsis: true, render: dash },
        {
            title: '预览',
            width: 80,
            align: 'center',
            render: (_, record) => record.equipmentPhotoFileId ? (_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => setPreviewTarget({
                    fileId: record.equipmentPhotoFileId,
                    fileName: record.equipmentPhotoFileName || '设备照片',
                    fileType: 'image/*',
                }), children: "\u9884\u89C8" })) : (_jsx(Typography.Text, { type: "secondary", children: "-" })),
        },
        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
        {
            title: '操作',
            width: 110,
            render: (_, record) => (_jsx(PermissionGuard, { codes: ['certification.preparation.equipment.bind', 'certification.preparation.equipment.assign'], mode: "any", children: _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u4EFB\u52A1\u8BBE\u5907\uFF1F", okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => removeMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) }) })),
        },
    ];
    const availableOptions = (availableEquipmentQuery.data || []).map((item) => ({
        label: `${item.equipmentType}（${item.assetNo}，${item.equipmentQuantity || 1}）`,
        value: item.id,
    }));
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(ProCard, { title: "\u4EFB\u52A1\u8BBE\u5907", extra: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), _jsx(PermissionGuard, { codes: ['certification.preparation.equipment.bind', 'certification.preparation.equipment.assign'], mode: "any", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !taskId, onClick: () => {
                                    assignForm.resetFields();
                                    setAssignModalOpen(true);
                                }, children: "\u7ED1\u5B9A\u8BBE\u5907" }) })] }), children: !taskId ? (_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u914D\u7F6E\u8BBE\u5907\u3002" })) : (_jsx(Table, { rowKey: "id", columns: columns, dataSource: taskEquipmentQuery.data || [], loading: taskEquipmentQuery.isFetching, pagination: false, scroll: { x: 840 } })) }), _jsx(FilePreviewModal, { open: Boolean(previewTarget), target: previewTarget, onClose: () => setPreviewTarget(null) }), _jsx(Modal, { title: "\u7ED1\u5B9A\u8BBE\u5907", open: assignModalOpen, onOk: () => assignForm.validateFields().then((values) => assignMutation.mutate(values)), onCancel: () => setAssignModalOpen(false), confirmLoading: assignMutation.isPending, width: 520, children: _jsxs(Form, { form: assignForm, layout: "vertical", children: [_jsx(Form.Item, { name: "equipmentId", label: "\u8BBE\u5907", rules: [{ required: true, message: '请选择设备' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", loading: availableEquipmentQuery.isFetching, placeholder: "\u8BF7\u9009\u62E9\u7A7A\u95F2\u4E2D\u7684\u8BBE\u5907", options: availableOptions, notFoundContent: availableEquipmentQuery.isFetching
                                    ? '加载中...'
                                    : '暂无可用设备，请先在信息管理中新增并审核通过' }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u53EF\u9009" }) })] }) })] }));
}
