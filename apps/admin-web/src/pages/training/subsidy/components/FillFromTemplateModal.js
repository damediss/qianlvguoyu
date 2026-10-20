import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Modal, Radio, Space, Table, Tag, Typography, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import * as trainApi from '../../../../api/training';
function canAutoFill(fmt) {
    const f = (fmt || '').toLowerCase();
    return f === 'docx' || f === 'xlsx' || f === 'xls';
}
function canAutoFillFileName(name) {
    const n = (name || '').toLowerCase();
    return n.endsWith('.docx') || n.endsWith('.xlsx') || n.endsWith('.xls');
}
export function FillFromTemplateModal({ open, onClose, subsidyId, classId, instanceId, instanceTitle, instanceHasFile, instanceFileName, hasUploadTemplate, uploadTemplateFileName, materialType, onFilled, }) {
    const [fillSource, setFillSource] = useState('library');
    const [selectedId, setSelectedId] = useState(null);
    const previewLooksFilled = !!instanceFileName
        && (/^\[(填数|模板)\]/i.test(instanceFileName) || instanceFileName.includes('花名册'));
    const canUseCurrent = instanceId != null
        && (!!hasUploadTemplate
            || (!!instanceHasFile
                && !previewLooksFilled
                && (!instanceFileName || canAutoFillFileName(instanceFileName))));
    const fillBaseName = uploadTemplateFileName || (canUseCurrent ? instanceFileName : null);
    const listQ = useQuery({
        queryKey: ['subsidy-material-templates', 'fill', classId, materialType],
        queryFn: () => trainApi.listSubsidyMaterialTemplates({
            classId: classId ?? undefined,
            status: 'active',
        }),
        enabled: open,
    });
    useEffect(() => {
        if (!open) {
            setSelectedId(null);
            setFillSource('library');
            return;
        }
        setFillSource(canUseCurrent ? 'current' : 'library');
    }, [open, canUseCurrent]);
    useEffect(() => {
        if (!open || fillSource !== 'library')
            return;
        const rows = listQ.data ?? [];
        const matchType = materialType
            ? rows.find((r) => canAutoFill(r.fileFormat) && r.materialType === materialType)
            : undefined;
        const prefer = matchType || rows.find((r) => canAutoFill(r.fileFormat));
        setSelectedId(prefer ? String(prefer.id) : null);
    }, [open, fillSource, listQ.data, materialType]);
    const rows = useMemo(() => {
        const all = listQ.data ?? [];
        if (!materialType)
            return all;
        const matched = all.filter((r) => r.materialType === materialType);
        const others = all.filter((r) => r.materialType !== materialType);
        return matched.length ? [...matched, ...others] : all;
    }, [listQ.data, materialType]);
    const selected = rows.find((r) => String(r.id) === selectedId) ?? null;
    const fillMu = useMutation({
        mutationFn: async () => {
            if (fillSource === 'current') {
                if (instanceId == null)
                    throw new Error('缺少材料');
                return trainApi.fillSubsidyMaterialInstance(instanceId, {
                    useCurrentFile: true,
                    rosterScope: 'certified',
                });
            }
            if (!selectedId)
                throw new Error('请选择模板');
            const body = { templateId: selectedId, rosterScope: 'certified' };
            if (instanceId != null) {
                return trainApi.fillSubsidyMaterialInstance(instanceId, body);
            }
            return trainApi.fillSubsidyFromTemplate(subsidyId, body);
        },
        onSuccess: (res) => {
            if (res.missingKeys?.length) {
                message.warning(res.message || '已生成，部分字段为空');
            }
            else {
                message.success(res.message || '已按模板生成');
            }
            onFilled?.(res.instanceId ?? instanceId ?? undefined);
            onClose();
        },
        onError: (e) => message.error(e.message || '填数失败'),
    });
    const okDisabled = fillSource === 'current'
        ? !canUseCurrent
        : !selectedId || !canAutoFill(selected?.fileFormat);
    return (_jsxs(Modal, { title: instanceId ? `从模板填数：${instanceTitle || '当前材料'}` : '选模板 → 自动填数 → 生成', open: open, onCancel: onClose, width: 800, destroyOnClose: true, footer: _jsxs(Space, { children: [_jsx(Button, { onClick: onClose, children: "\u53D6\u6D88" }), _jsx(Button, { type: "primary", disabled: okDisabled, loading: fillMu.isPending, onClick: () => fillMu.mutate(), children: fillSource === 'current' ? '按上传模板填数' : '生成并填数' })] }), children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u7533\u62A5\u8865\u8D34\u4EC5\u7EB3\u5165\u5DF2\u83B7\u8BC1\u4EBA\u5458\u3002\u65E0 ${} \u5360\u4F4D\u7B26\u7684\u7533\u8BF7\u8868\u4E5F\u4F1A\u6309\u300C\u57F9\u8BAD\u673A\u6784\u540D\u79F0\u300D\u7B49\u4E2D\u6587\u6807\u7B7E\u81EA\u52A8\u5339\u914D\u586B\u53F3\u4FA7\u7A7A\u683C\uFF1B\u4F18\u5148\u7528\u6700\u8FD1\u4E00\u6B21\u300C\u66FF\u6362\u4E0A\u4F20\u300D\u5E95\u7A3F\u3002" }), instanceId ? (_jsxs("div", { style: { marginBottom: 12 }, children: [_jsx(Typography.Text, { type: "secondary", style: { display: 'block', marginBottom: 8 }, children: "\u5C06\u5199\u5165\u5F53\u524D\u6750\u6599\u7684\u65B0\u7248\u672C\uFF08\u4FDD\u7559\u5386\u53F2\uFF09\uFF0C\u4E0D\u65B0\u5EFA\u6E05\u5355\u9879\u3002" }), _jsxs(Radio.Group, { value: fillSource, onChange: (e) => setFillSource(e.target.value), optionType: "button", buttonStyle: "solid", children: [_jsx(Radio.Button, { value: "current", disabled: !canUseCurrent, children: "\u57FA\u4E8E\u4E0A\u4F20\u6A21\u677F" }), _jsx(Radio.Button, { value: "library", children: "\u4ECE\u6A21\u677F\u5E93\u9009\u62E9" })] }), fillSource === 'current' ? (_jsxs(Typography.Paragraph, { style: { marginTop: 10, marginBottom: 0 }, children: ["\u586B\u6570\u5E95\u7A3F\uFF1A", _jsx(Typography.Text, { code: true, children: fillBaseName || '未知' }), _jsx("br", {}), _jsx(Typography.Text, { type: "secondary", children: "\u4F7F\u7528\u6700\u8FD1\u4E00\u6B21\u300C\u66FF\u6362\u4E0A\u4F20\u300D\u7684\u6587\u4EF6\u5199\u5165\u672C\u73ED\u6570\u636E\uFF08\u4E0D\u662F\u53F3\u4FA7\u9884\u89C8\u91CC\u5DF2\u751F\u6210\u7684 [\u586B\u6570] \u6587\u4EF6\uFF09\u3002" })] })) : null, previewLooksFilled && fillSource === 'current' ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginTop: 10 }, message: "\u53F3\u4FA7\u9884\u89C8\u6587\u4EF6\u540D\u50CF\u586B\u6570\u7ED3\u679C\uFF0C\u7CFB\u7EDF\u4E0D\u4F1A\u62FF\u5B83\u5F53\u6A21\u677F\uFF1B\u8BF7\u786E\u8BA4\u66FE\u300C\u66FF\u6362\u4E0A\u4F20\u300D\u8FC7\u6B63\u786E\u7684\u7533\u8BF7\u8868\u7A7A\u767D\u6A21\u677F\u3002" })) : null, !canUseCurrent ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginTop: 10 }, message: "\u5C1A\u65E0\u53EF\u7528\u7684\u4E0A\u4F20\u6A21\u677F\u3002\u8BF7\u5148\u5BF9\u5F53\u524D\u6750\u6599\u300C\u66FF\u6362\u4E0A\u4F20\u300D\u6B63\u786E\u7684\u7533\u8BF7\u8868\uFF08docx/xlsx\uFF09\uFF0C\u518D\u56DE\u6765\u586B\u6570\uFF1B\u6216\u6539\u4ECE\u6A21\u677F\u5E93\u9009\u62E9\u5339\u914D\u7C7B\u578B\u7684\u6A21\u677F\u3002" })) : null] })) : (_jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 8 }, children: "\u5C06\u5728\u672C\u73ED\u6750\u6599\u6E05\u5355\u4E2D\u65B0\u589E\u4E00\u9879\uFF08\u7C7B\u578B\u53D6\u81EA\u6A21\u677F\uFF09\u3002" })), fillSource === 'library' || !instanceId ? (_jsx(Table, { rowKey: (r) => String(r.id), size: "small", loading: listQ.isLoading, dataSource: rows, pagination: false, scroll: { y: 320 }, onRow: (row) => ({
                    onClick: () => {
                        if (canAutoFill(row.fileFormat))
                            setSelectedId(String(row.id));
                    },
                    style: {
                        cursor: canAutoFill(row.fileFormat) ? 'pointer' : 'not-allowed',
                        opacity: canAutoFill(row.fileFormat) ? 1 : 0.55,
                    },
                }), columns: [
                    {
                        title: '',
                        width: 48,
                        render: (_, row) => (_jsx(Radio, { disabled: !canAutoFill(row.fileFormat), checked: selectedId === String(row.id), onChange: () => setSelectedId(String(row.id)) })),
                    },
                    { title: '标题', dataIndex: 'title', ellipsis: true },
                    {
                        title: '类型',
                        dataIndex: 'materialTypeLabel',
                        width: 88,
                        render: (label, row) => materialType && row.materialType === materialType ? (_jsx(Tag, { color: "blue", children: label || row.materialType })) : (label || row.materialType || '—'),
                    },
                    {
                        title: '格式',
                        dataIndex: 'fileFormat',
                        width: 72,
                        render: (v) => _jsx(Tag, { color: canAutoFill(v) ? 'blue' : 'default', children: v }),
                    },
                    {
                        title: '占位符',
                        dataIndex: 'fieldKeys',
                        ellipsis: true,
                        render: (keys) => keys?.length ? keys.slice(0, 5).join(', ') + (keys.length > 5 ? '…' : '') : '—',
                    },
                ] })) : null, fillSource === 'library' && !rows.length && !listQ.isLoading ? (_jsx(Typography.Paragraph, { type: "secondary", style: { marginTop: 12 }, children: "\u6682\u65E0\u6A21\u677F\uFF0C\u8BF7\u5148\u6253\u5F00\u300C\u6A21\u677F\u5E93\u300D\u4E0A\u4F20 docx / xlsx \u586B\u6570\u6A21\u677F\u3002" })) : null] }));
}
