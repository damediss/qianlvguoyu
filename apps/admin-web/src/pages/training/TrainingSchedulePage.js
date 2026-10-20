import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState, useEffect } from 'react';
import { Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, ExportOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { downloadUrlFile } from '../../api/files';
import { TrainingProcessSteps } from './TrainingProcessSteps';
export function TrainingSchedulePage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [classId, setClassId] = useState(0);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingIndex, setEditingIndex] = useState(null);
    const [scheduleItems, setScheduleItems] = useState([]);
    const { data: scheduleData, isLoading } = useQuery({
        queryKey: ['class-schedule', classId],
        queryFn: () => trainApi.getClassSchedule(classId),
        enabled: classId > 0,
    });
    useEffect(() => {
        if (scheduleData)
            setScheduleItems(scheduleData);
    }, [scheduleData]);
    const { data: classData } = useQuery({
        queryKey: ['info-classes', 'schedule-all'],
        queryFn: async () => {
            const pageSize = 200;
            const maxPages = 50;
            const list = [];
            let page = 1;
            let total = Number.POSITIVE_INFINITY;
            while (list.length < total && page <= maxPages) {
                const res = await trainApi.pageInfoClassList({ page, pageSize });
                const batch = res.list ?? [];
                list.push(...batch);
                total = typeof res.total === 'number' ? res.total : list.length;
                if (batch.length === 0)
                    break;
                page += 1;
            }
            return { list, total: list.length };
        },
    });
    const saveMu = useMutation({
        mutationFn: () => trainApi.saveClassSchedule(classId, scheduleItems),
        onSuccess: () => { message.success('课表保存成功'); queryClient.invalidateQueries({ queryKey: ['class-schedule', classId] }); },
        onError: () => message.error('保存失败'),
    });
    const exportMu = useMutation({
        mutationFn: () => trainApi.exportClassSchedule(classId),
        onSuccess: async (data) => {
            if (data.downUrl) {
                await downloadUrlFile(data.downUrl, data.fileName);
            }
            message.success('导出成功');
        },
        onError: () => message.error('导出失败'),
    });
    const openAdd = () => { form.resetFields(); setEditingIndex(null); setModalOpen(true); };
    const openEdit = (index) => { setEditingIndex(index); form.setFieldsValue(scheduleItems[index]); setModalOpen(true); };
    const handleDelete = (index) => setScheduleItems(scheduleItems.filter((_, i) => i !== index));
    const handleSubmit = async () => {
        const v = await form.validateFields();
        const item = { ...v, scheduleDate: v.scheduleDate ? dayjs(v.scheduleDate).format('YYYY-MM-DD') : undefined };
        if (editingIndex !== null) {
            const newItems = [...scheduleItems];
            newItems[editingIndex] = item;
            setScheduleItems(newItems);
        }
        else {
            setScheduleItems([...scheduleItems, item]);
        }
        setModalOpen(false);
        form.resetFields();
    };
    const columns = [
        { title: '序号', key: 'idx', width: 60, render: (_, __, i) => i + 1 },
        { title: '日期', dataIndex: 'scheduleDate', width: 120 },
        { title: '星期', dataIndex: 'dayOfWeek', width: 70, render: (v) => ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][v ?? 0] ?? '' },
        { title: '课程名称', dataIndex: 'courseName', width: 180 },
        { title: '讲师', dataIndex: 'teacherName', width: 120 },
        { title: '教室', dataIndex: 'roomName', width: 120 },
        { title: '开始时间', dataIndex: 'startTime', width: 100 },
        { title: '结束时间', dataIndex: 'endTime', width: 100 },
        {
            title: '操作', key: 'act', width: 120, fixed: 'right',
            render: (_, __, i) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(i), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => handleDelete(i), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    const classOptions = (classData?.list ?? []).map((c) => ({
        value: c.id,
        label: `${c.title} (${c.train_title ?? ''})`,
    }));
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u8BFE\u8868\u914D\u7F6E" }), _jsx(Typography.Text, { type: "secondary", children: "\u4E3A\u73ED\u7EA7\u914D\u7F6E\u57F9\u8BAD\u8BFE\u7A0B\u5B89\u6392\uFF0C\u652F\u6301\u6DFB\u52A0\u3001\u7F16\u8F91\u548C\u5BFC\u51FA\u8BFE\u8868" })] }) }), _jsx(TrainingProcessSteps, { current: 5 }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Space, { children: [_jsx("span", { children: "\u9009\u62E9\u73ED\u7EA7\uFF1A" }), _jsx(Select, { showSearch: true, allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u8981\u6392\u8BFE\u7684\u73ED\u7EA7", style: { width: 360 }, options: classOptions, value: classId || undefined, onChange: (v) => setClassId(v ?? 0), filterOption: (input, option) => (option?.label ?? '').includes(input) }), _jsx(Button, { type: "primary", disabled: classId <= 0, icon: _jsx(PlusOutlined, {}), onClick: openAdd, children: "\u6DFB\u52A0\u8BFE\u65F6" }), _jsx(Button, { disabled: classId <= 0 || scheduleItems.length === 0, icon: _jsx(ReloadOutlined, {}), loading: saveMu.isPending, onClick: () => saveMu.mutate(), children: "\u4FDD\u5B58\u8BFE\u8868" }), _jsx(Button, { disabled: classId <= 0 || scheduleItems.length === 0, icon: _jsx(ExportOutlined, {}), loading: exportMu.isPending, onClick: () => exportMu.mutate(), children: "\u5BFC\u51FACSV" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['class-schedule', classId] }), children: "\u5237\u65B0" })] }) }), _jsx(Card, { size: "small", children: _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: scheduleItems, pagination: false, scroll: { x: 900 }, locale: { emptyText: classId <= 0 ? '请先选择班级' : '暂无课表数据，请添加课时' } }) }), _jsx(Modal, { title: editingIndex !== null ? '编辑课时' : '添加课时', open: modalOpen, onOk: handleSubmit, onCancel: () => { setModalOpen(false); form.resetFields(); }, destroyOnHidden: true, width: 560, children: _jsx(Form, { form: form, layout: "vertical", children: _jsxs(Space, { style: { width: '100%' }, direction: "vertical", children: [_jsx(Form.Item, { name: "scheduleDate", label: "\u65E5\u671F", rules: [{ required: true }], children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "dayOfWeek", label: "\u661F\u671F", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9", allowClear: true, options: [1, 2, 3, 4, 5, 6, 7].map(d => ({ value: d, label: `周${['一', '二', '三', '四', '五', '六', '日'][d - 1]}` })) }) }), _jsx(Form.Item, { name: "courseName", label: "\u8BFE\u7A0B\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8BFE\u7A0B\u540D\u79F0" }) }), _jsx(Form.Item, { name: "teacherName", label: "\u8BB2\u5E08", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8BB2\u5E08\u59D3\u540D" }) }), _jsx(Form.Item, { name: "roomName", label: "\u6559\u5BA4", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u6559\u5BA4\u540D\u79F0" }) }), _jsxs(Space, { children: [_jsx(Form.Item, { name: "startTime", label: "\u5F00\u59CB\u65F6\u95F4", rules: [{ required: true }], children: _jsx(Input, { placeholder: "08:00", style: { width: 140 } }) }), _jsx(Form.Item, { name: "endTime", label: "\u7ED3\u675F\u65F6\u95F4", rules: [{ required: true }], children: _jsx(Input, { placeholder: "10:00", style: { width: 140 } }) })] }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9", allowClear: true, options: [{ value: 0, label: '未开始' }, { value: 1, label: '进行中' }, { value: 2, label: '已完成' }] }) })] }) }) })] }));
}
