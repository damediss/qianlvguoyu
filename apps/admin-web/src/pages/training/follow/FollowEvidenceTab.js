import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 跟班 · 课堂影像留痕（手动添加）+ 培训照片生成
 * 不再按课表齐套；管理员直接添加图片并填写图注。
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { AutoComplete, Button, Card, DatePicker, Empty, Form, Image, Input, Modal, Select, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { DeleteOutlined, DownloadOutlined, PlusOutlined, ReloadOutlined, ThunderboltOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { ApiAuthImage } from '../../../components/ApiAuthImage';
import { downloadUrlFile } from '../../../api/files';
import * as trainApi from '../../../api/training';
const CAPTION_OPTIONS = [
    { value: '开班' },
    { value: '公共课' },
    { value: '实操课' },
];
export function FollowEvidenceTab({ classId }) {
    const qc = useQueryClient();
    const [addOpen, setAddOpen] = useState(false);
    const [genOpen, setGenOpen] = useState(false);
    const [caption, setCaption] = useState('');
    const [bizDate, setBizDate] = useState(dayjs());
    const [dayPeriod, setDayPeriod] = useState('上午');
    const [pendingFile, setPendingFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);
    const [genForm] = Form.useForm();
    const clearPendingImage = () => {
        setPreviewUrl((prev) => {
            if (prev)
                URL.revokeObjectURL(prev);
            return null;
        });
        setPendingFile(null);
    };
    const resetAddForm = () => {
        setCaption('');
        setBizDate(dayjs());
        setDayPeriod('上午');
        clearPendingImage();
    };
    const closeAddModal = () => {
        setAddOpen(false);
        resetAddForm();
    };
    const listQ = useQuery({
        queryKey: ['follow-evidences-manual', classId],
        queryFn: () => trainApi.listFollowEvidences(classId, {
            mediaType: 'photo',
        }),
    });
    const latestPackQ = useQuery({
        queryKey: ['follow-photo-pack-latest', classId],
        queryFn: () => trainApi.getLatestFollowPhotoPack(classId),
    });
    const invalidate = () => {
        void qc.invalidateQueries({ queryKey: ['follow-evidences-manual', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-photo-pack-latest', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-evidence-coverage', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-quality-metrics', classId] });
    };
    const photos = useMemo(() => {
        const list = [...(listQ.data ?? [])].filter((r) => r.mediaType !== 'video');
        list.sort((a, b) => {
            const da = String(a.bizDate ?? '');
            const db = String(b.bizDate ?? '');
            if (da !== db)
                return da.localeCompare(db);
            const sa = Number(a.sortNo ?? 0);
            const sb = Number(b.sortNo ?? 0);
            if (sa !== sb)
                return sa - sb;
            return String(a.id).localeCompare(String(b.id), undefined, { numeric: true });
        });
        return list;
    }, [listQ.data]);
    const addMu = useMutation({
        mutationFn: async (payload) => {
            return trainApi.uploadFollowEvidence(classId, payload.file, {
                bizDate: payload.bizDate.format('YYYY-MM-DD'),
                caption: payload.caption.trim(),
                teachFormOverride: payload.dayPeriod,
                mediaType: 'photo',
            });
        },
        onSuccess: () => {
            message.success('已添加');
            closeAddModal();
            invalidate();
        },
        onError: (e) => message.error(e.message || '添加失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteFollowEvidence(id),
        onSuccess: () => {
            message.success('已删除');
            invalidate();
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    const pickImageFile = (file) => {
        if (!file.type.startsWith('image/')) {
            message.warning('请选择图片文件');
            return;
        }
        setPreviewUrl((prev) => {
            if (prev)
                URL.revokeObjectURL(prev);
            return URL.createObjectURL(file);
        });
        setPendingFile(file);
    };
    const extractClipboardImage = (clipboardData) => {
        if (!clipboardData)
            return null;
        const items = clipboardData.items;
        for (let i = 0; i < items.length; i += 1) {
            const item = items[i];
            if (!item.type.startsWith('image/'))
                continue;
            const blob = item.getAsFile();
            if (!blob)
                continue;
            const ext = item.type.includes('png')
                ? 'png'
                : item.type.includes('webp')
                    ? 'webp'
                    : item.type.includes('gif')
                        ? 'gif'
                        : 'jpg';
            return new File([blob], blob.name || `paste-${Date.now()}.${ext}`, {
                type: blob.type || item.type || 'image/png',
            });
        }
        return null;
    };
    const pickImageRef = useRef(pickImageFile);
    pickImageRef.current = pickImageFile;
    useEffect(() => {
        if (!addOpen)
            return;
        const onPaste = (e) => {
            const file = extractClipboardImage(e.clipboardData);
            if (!file)
                return;
            e.preventDefault();
            pickImageRef.current(file);
        };
        window.addEventListener('paste', onPaste);
        return () => window.removeEventListener('paste', onPaste);
    }, [addOpen]);
    useEffect(() => {
        return () => {
            if (previewUrl)
                URL.revokeObjectURL(previewUrl);
        };
    }, [previewUrl]);
    const confirmAdd = () => {
        if (!pendingFile) {
            message.warning('请先选择或粘贴图片');
            return;
        }
        if (!bizDate) {
            message.warning('请选择日期');
            return;
        }
        if (!caption.trim()) {
            message.warning('请填写图注说明');
            return;
        }
        addMu.mutate({
            file: pendingFile,
            caption: caption.trim(),
            bizDate,
            dayPeriod,
        });
    };
    const genMu = useMutation({
        mutationFn: (payload) => trainApi.generateFollowPhotoPack(classId, payload),
        onSuccess: () => {
            message.success('培训照片已生成，可点击「下载照片」获取文件');
            setGenOpen(false);
            invalidate();
        },
        onError: (e) => message.error(e.message || '生成失败'),
    });
    const downloadMu = useMutation({
        mutationFn: async () => {
            const res = await trainApi.getLatestFollowPhotoPack(classId);
            if (!res.exists) {
                throw new Error('尚未生成培训照片，请先点击「生成培训照片」');
            }
            const path = res.filePath || res.url;
            if (!path) {
                throw new Error('培训照片文件路径缺失');
            }
            await downloadUrlFile(path, `${res.title || '培训照片'}.docx`);
            return res;
        },
        onSuccess: () => message.success('开始下载培训照片'),
        onError: (e) => message.error(e.message || '下载失败'),
    });
    const openGenerateModal = async () => {
        setGenOpen(true);
        try {
            const defaults = await trainApi.getFollowPhotoPackDefaults(classId);
            genForm.setFieldsValue({
                title: defaults.title || '',
                workType: defaults.workType || '',
                level: defaults.level || '',
                trainTime: defaults.trainTime || '',
                trainUnit: defaults.trainUnit || '',
                trainAddress: defaults.trainAddress || '',
            });
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '加载默认信息失败');
        }
    };
    const submitGenerate = async () => {
        const values = await genForm.validateFields();
        if (!String(values.title || '').trim()) {
            message.warning('请填写标题');
            return;
        }
        genMu.mutate({
            title: String(values.title || '').trim(),
            workType: String(values.workType || '').trim(),
            level: String(values.level || '').trim(),
            trainTime: String(values.trainTime || '').trim(),
            trainUnit: String(values.trainUnit || '').trim(),
            trainAddress: String(values.trainAddress || '').trim(),
        });
    };
    const periodLabel = (row) => {
        const t = String(row.teachFormOverride || row.teachForm || '').trim();
        if (t === '上午' || t === '下午' || t === '全天')
            return t;
        return '—';
    };
    const columns = [
        {
            title: '序号',
            width: 70,
            render: (_, __, index) => `图${index + 1}`,
        },
        {
            title: '预览',
            width: 100,
            render: (_, row) => row.id ? (_jsx(Image.PreviewGroup, { children: _jsx(ApiAuthImage, { url: `/training/follow-evidences/${row.id}/preview`, width: 72, height: 54, style: { objectFit: 'cover', borderRadius: 4 } }) })) : ('—'),
        },
        {
            title: '图注说明',
            dataIndex: 'caption',
            ellipsis: true,
            render: (v) => v || '—',
        },
        {
            title: '日期',
            width: 120,
            dataIndex: 'bizDate',
            render: (v) => v || '—',
        },
        {
            title: '时段',
            width: 80,
            render: (_, row) => periodLabel(row),
        },
        {
            title: '状态',
            width: 90,
            render: (_, row) => row.reviewStatus === 'approved' ? (_jsx(Tag, { color: "success", children: "\u5DF2\u901A\u8FC7" })) : (_jsx(Tag, { children: row.reviewStatus || '—' })),
        },
        {
            title: '操作',
            width: 90,
            fixed: 'right',
            render: (_, row) => (_jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => {
                    Modal.confirm({
                        title: '删除该照片？',
                        onOk: () => deleteMu.mutateAsync(row.id),
                    });
                }, children: "\u5220\u9664" })),
        },
    ];
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsx(Card, { size: "small", title: `已添加照片（${photos.length}）`, extra: _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => invalidate(), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                                resetAddForm();
                                setAddOpen(true);
                            }, children: "\u6DFB\u52A0\u7167\u7247" }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(ThunderboltOutlined, {}), disabled: !photos.length, loading: genMu.isPending, onClick: () => void openGenerateModal(), children: "\u751F\u6210\u57F9\u8BAD\u7167\u7247" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), disabled: !latestPackQ.data?.exists, loading: downloadMu.isPending || latestPackQ.isFetching, onClick: () => downloadMu.mutate(), children: "\u4E0B\u8F7D\u7167\u7247" })] }), children: _jsx(Table, { size: "small", rowKey: (r) => String(r.id), loading: listQ.isFetching, columns: columns, dataSource: photos, pagination: { pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 张` }, locale: {
                        emptyText: _jsx(Empty, { description: "\u6682\u65E0\u7167\u7247\uFF0C\u8BF7\u70B9\u51FB\u300C\u6DFB\u52A0\u7167\u7247\u300D" }),
                    }, scroll: { x: 720 } }) }), _jsx(Modal, { title: "\u6DFB\u52A0\u7167\u7247", open: addOpen, onCancel: closeAddModal, destroyOnHidden: true, okText: "\u786E\u8BA4\u6DFB\u52A0", confirmLoading: addMu.isPending, onOk: confirmAdd, okButtonProps: { disabled: !pendingFile }, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u56FE\u6CE8\u8BF4\u660E" }), _jsx(AutoComplete, { style: { width: '100%', marginTop: 8 }, options: CAPTION_OPTIONS, placeholder: "\u9009\u62E9\u6216\u8F93\u5165\uFF0C\u5982\uFF1A\u5F00\u73ED", value: caption, onChange: (v) => setCaption(String(v ?? '').slice(0, 100)), filterOption: (input, option) => String(option?.value ?? '')
                                        .toLowerCase()
                                        .includes(input.trim().toLowerCase()), allowClear: true })] }), _jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u65E5\u671F" }), _jsx("div", { style: { marginTop: 8 }, children: _jsx(DatePicker, { style: { width: '100%' }, value: bizDate, onChange: (d) => setBizDate(d), allowClear: false }) })] }), _jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u65F6\u6BB5" }), _jsx(Select, { style: { width: '100%', marginTop: 8 }, value: dayPeriod, onChange: setDayPeriod, options: [
                                        { value: '上午', label: '上午' },
                                        { value: '下午', label: '下午' },
                                        { value: '全天', label: '全天' },
                                    ] })] }), _jsx("div", { style: {
                                border: '1px dashed #d9d9d9',
                                borderRadius: 8,
                                padding: 12,
                                textAlign: 'center',
                                background: '#fafafa',
                            }, children: previewUrl ? (_jsxs("div", { children: [_jsx(Image, { src: previewUrl, alt: "\u9884\u89C8", style: { maxHeight: 220, objectFit: 'contain' } }), _jsx("div", { style: { marginTop: 12 }, children: _jsxs(Space, { children: [_jsx(Upload, { accept: "image/*", showUploadList: false, beforeUpload: (file) => {
                                                        pickImageFile(file);
                                                        return false;
                                                    }, children: _jsx(Button, { children: "\u91CD\u65B0\u9009\u62E9" }) }), _jsx(Button, { danger: true, onClick: clearPendingImage, children: "\u6E05\u9664\u56FE\u7247" })] }) })] })) : (_jsxs(_Fragment, { children: [_jsx(Typography.Text, { type: "secondary", children: "\u5148\u9009\u62E9\u6216\u7C98\u8D34\u56FE\u7247\uFF08Ctrl+V / \u2318+V\uFF09\uFF0C\u518D\u586B\u5199\u8BF4\u660E\u540E\u786E\u8BA4\u6DFB\u52A0" }), _jsx("div", { style: { marginTop: 12 }, children: _jsx(Upload, { accept: "image/*", showUploadList: false, beforeUpload: (file) => {
                                                pickImageFile(file);
                                                return false;
                                            }, children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), children: "\u9009\u62E9\u56FE\u7247" }) }) })] })) })] }) }), _jsx(Modal, { title: "\u751F\u6210\u57F9\u8BAD\u7167\u7247", open: genOpen, onCancel: () => setGenOpen(false), okText: "\u751F\u6210", confirmLoading: genMu.isPending, onOk: () => void submitGenerate(), destroyOnHidden: true, width: 560, children: _jsxs(Form, { form: genForm, layout: "vertical", style: { marginTop: 8 }, children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, message: '请填写标题' }], children: _jsx(Input, { placeholder: "\u5982\uFF1A\u534E\u53CB\u9502\u4E1A\u6D78\u51FA\u5DE5\u9AD8\u7EA7\u57F9\u8BAD\u7167\u7247", maxLength: 120 }) }), _jsx(Form.Item, { name: "workType", label: "\u57F9\u8BAD\u5DE5\u79CD", children: _jsx(Input, { placeholder: "\u5982\uFF1A\u6D78\u51FA\u5DE5", maxLength: 80 }) }), _jsx(Form.Item, { name: "level", label: "\u57F9\u8BAD\u7EA7\u522B", children: _jsx(Input, { placeholder: "\u5982\uFF1A\u9AD8\u7EA7\u5DE5", maxLength: 40 }) }), _jsx(Form.Item, { name: "trainTime", label: "\u57F9\u8BAD\u65F6\u95F4", children: _jsx(Input, { placeholder: "\u5982\uFF1A2024-06-01\u81F32024-06-30", maxLength: 80 }) }), _jsx(Form.Item, { name: "trainUnit", label: "\u57F9\u8BAD\u5355\u4F4D", children: _jsx(Input, { placeholder: "\u57F9\u8BAD\u673A\u6784\u540D\u79F0", maxLength: 120 }) }), _jsx(Form.Item, { name: "trainAddress", label: "\u57F9\u8BAD\u5730\u5740", children: _jsx(Input, { placeholder: "\u57F9\u8BAD\u5730\u70B9", maxLength: 200 }) })] }) })] }));
}
