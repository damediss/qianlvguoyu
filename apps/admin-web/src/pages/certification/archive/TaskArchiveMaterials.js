import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Empty, Form, message, Select, Space, Table, Upload } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteArchiveMaterial, downloadArchiveFile, listArchiveMaterials, uploadArchiveMaterial } from '../../../api/certificationArchive';
import { fileSizeText, taskMaterialOptions, dateTimeText } from './shared';
export function TaskArchiveMaterials({ archiveId }) {
    const [form] = Form.useForm();
    const queryClient = useQueryClient();
    const query = useQuery({
        queryKey: ['certification-archive-materials', archiveId],
        queryFn: () => listArchiveMaterials(archiveId),
        enabled: !!archiveId,
    });
    const uploadedTypes = new Set((query.data || []).map((item) => item.materialType));
    const materialOptions = taskMaterialOptions.map((option) => ({
        ...option,
        disabled: uploadedTypes.has(option.value),
    }));
    const uploadMutation = useMutation({
        mutationFn: async (values) => {
            const option = taskMaterialOptions.find((item) => item.value === values.materialType);
            const data = new FormData();
            data.append('materialType', values.materialType);
            data.append('materialName', option?.label || values.materialType);
            data.append('file', values.file);
            return uploadArchiveMaterial(archiveId, data);
        },
        onSuccess: () => {
            message.success('材料已上传');
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['certification-archive-materials', archiveId] });
            queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (id) => deleteArchiveMaterial(archiveId, id),
        onSuccess: () => {
            message.success('材料已删除');
            queryClient.invalidateQueries({ queryKey: ['certification-archive-materials', archiveId] });
            queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
        },
    });
    const columns = [
        { title: '材料名称', dataIndex: 'materialName', width: 260 },
        { title: '材料类型', dataIndex: 'materialType', width: 180 },
        { title: '材料分组', dataIndex: 'materialGroup', width: 130 },
        { title: '文件名', dataIndex: 'fileName', width: 220, ellipsis: true },
        { title: '大小', dataIndex: 'fileSize', width: 100, render: fileSizeText },
        { title: '上传时间', dataIndex: 'uploadedAt', width: 170, render: dateTimeText },
        {
            title: '操作',
            width: 160,
            render: (_, row) => (_jsxs(Space, { children: [_jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), onClick: () => downloadArchiveFile(row.id, row.fileName), children: "\u4E0B\u8F7D" }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => deleteMutation.mutate(row.id), children: "\u5220\u9664" })] })),
        },
    ];
    if (!archiveId) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u5F52\u6863\u8BB0\u5F55" });
    }
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Alert, { type: uploadedTypes.size === taskMaterialOptions.length ? 'success' : 'info', showIcon: true, message: `认定材料已上传 ${uploadedTypes.size} / ${taskMaterialOptions.length} 类`, description: "\u6BCF\u7C7B\u6750\u6599\u5728\u5F53\u524D\u4EFB\u52A1\u4E2D\u53EA\u80FD\u4E0A\u4F20\u4E00\u6B21\uFF1B\u5982\u9700\u91CD\u65B0\u4E0A\u4F20\uFF0C\u8BF7\u5148\u5220\u9664\u539F\u6750\u6599\u3002" }), _jsxs(Form, { form: form, layout: "inline", onFinish: uploadMutation.mutate, children: [_jsx(Form.Item, { name: "materialType", rules: [{ required: true, message: '请选择材料类型' }], children: _jsx(Select, { placeholder: "\u6750\u6599\u7C7B\u578B", style: { width: 320 }, options: materialOptions }) }), _jsx(Form.Item, { name: "file", valuePropName: "file", getValueFromEvent: (event) => event?.fileList?.[0]?.originFileObj, rules: [{ required: true, message: '请选择文件' }], children: _jsx(Upload, { beforeUpload: () => false, maxCount: 1, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), children: "\u9009\u62E9\u6587\u4EF6" }) }) }), _jsx(Button, { type: "primary", htmlType: "submit", disabled: uploadedTypes.size === taskMaterialOptions.length, loading: uploadMutation.isPending, children: "\u4E0A\u4F20" })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: query.data || [], loading: query.isLoading, pagination: false, scroll: { x: 1100 } })] }));
}
