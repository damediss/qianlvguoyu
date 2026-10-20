import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { Button, Empty, List, Space, Typography, Upload, message } from 'antd';
import { ArrowLeftOutlined, EditOutlined, FileOutlined } from '@ant-design/icons';
import * as trainApi from '../../../../api/training';
import { downloadUrlFile, fetchPlatformFileBlob, fetchUrlFileBlob, resolveFilePreviewKind, } from '../../../../api/files';
import { MaterialStatusTag } from './MaterialStatusTag';
import { MaterialFilePreview } from './MaterialFilePreview';
import { MaterialSpreadsheetEditor } from './MaterialSpreadsheetEditor';
import { DocxMetaEditor } from './DocxMetaEditor';
import { DocxOfficeEditor } from './DocxOfficeEditor';
function isSpreadsheet(name, path) {
    const s = `${name || ''} ${path || ''}`.toLowerCase();
    return /\.(xlsx?|csv)(\?|$)/i.test(s);
}
function isWordDoc(name, path) {
    const s = `${name || ''} ${path || ''}`.toLowerCase();
    return /\.docx?(\?|$)/i.test(s);
}
/** 申请表/花名册/汇总等默认可进 Office 在线编辑（文件名常无扩展名） */
function isOfficeEditableMaterialType(materialType) {
    return (materialType === 'application_form'
        || materialType === 'roster'
        || materialType === 'summary'
        || materialType === 'other');
}
/** 证书 PDF 等多文件并列；申请表/花名册等即使 slotFiles 有历史记录也按当前版本单文件处理 */
function isMultiFileMaterial(item) {
    if (item.materialType === 'certificate')
        return true;
    const slots = item.slotFiles || [];
    if (slots.length <= 1)
        return false;
    const mainName = item.fileName || '';
    if (isSpreadsheet(mainName) || isWordDoc(mainName))
        return false;
    if (isOfficeEditableMaterialType(item.materialType))
        return false;
    return slots.length > 1;
}
function buildSlotTargets(item) {
    const slots = item.slotFiles || [];
    if (isMultiFileMaterial(item) && slots.length > 0) {
        return slots.map((f, idx) => ({
            key: String(f.id ?? `slot-${idx}`),
            fileId: f.fileId,
            filePath: f.filePath,
            fileName: f.fileName || `文件${idx + 1}`,
        }));
    }
    if (item.fileId || item.filePath || item.photoPackPath) {
        return [
            {
                key: 'main',
                fileId: item.fileId,
                filePath: item.filePath || item.photoPackPath,
                fileName: item.fileName || item.photoPackTitle || item.title,
            },
        ];
    }
    if (slots.length === 1) {
        const f = slots[0];
        return [
            {
                key: String(f.id ?? 'slot-0'),
                fileId: f.fileId,
                filePath: f.filePath,
                fileName: f.fileName || item.title,
            },
        ];
    }
    return [];
}
export function MaterialWorkspacePanel({ item, onConfirm, confirmLoading, onReplaceFile, replaceLoading, onFillFromTemplate, onOfficeSaved, }) {
    const [selectedKey, setSelectedKey] = useState(null);
    const [editing, setEditing] = useState(false);
    const [savingCells, setSavingCells] = useState(false);
    const [wordEditorMode, setWordEditorMode] = useState('meta');
    /** 文件名无扩展名时，用 MIME 嗅探结果决定编辑器 */
    const [sniffedKind, setSniffedKind] = useState(null);
    /** OnlyOffice 是否可用（未部署则不展示入口） */
    const [onlyOfficeAvailable, setOnlyOfficeAvailable] = useState(false);
    const targets = useMemo(() => (item ? buildSlotTargets(item) : []), [item]);
    useEffect(() => {
        setSelectedKey(null);
        setEditing(false);
        setWordEditorMode('meta');
        setSniffedKind(null);
        setOnlyOfficeAvailable(false);
    }, [item?.id]);
    useEffect(() => {
        if (!item?.hasFile || !item?.id) {
            setOnlyOfficeAvailable(false);
            return;
        }
        let cancelled = false;
        void trainApi
            .getSubsidyMaterialOfficeConfig(item.id)
            .then((cfg) => {
            if (!cancelled) {
                const ok = !!cfg.enabled && !!cfg.config && !!cfg.apiScriptUrl;
                setOnlyOfficeAvailable(ok);
                if (!ok)
                    setWordEditorMode('meta');
            }
        })
            .catch(() => {
            if (!cancelled) {
                setOnlyOfficeAvailable(false);
                setWordEditorMode('meta');
            }
        });
        return () => {
            cancelled = true;
        };
    }, [item?.id, item?.hasFile]);
    const multiFile = !!item && isMultiFileMaterial(item) && targets.length > 1;
    const active = item == null
        ? null
        : selectedKey != null
            ? targets.find((t) => t.key === selectedKey) ?? null
            : multiFile
                ? null
                : targets[0] ?? null;
    useEffect(() => {
        if (!item?.hasFile || !active || multiFile) {
            setSniffedKind(null);
            return;
        }
        const nameHint = `${active.fileName || ''} ${item.fileName || ''} ${item.uploadTemplateFileName || ''}`;
        if (isWordDoc(nameHint) || isSpreadsheet(nameHint)) {
            setSniffedKind(isWordDoc(nameHint) ? 'docx' : 'spreadsheet');
            return;
        }
        let cancelled = false;
        const sniff = async () => {
            try {
                const blob = active.filePath
                    ? await fetchUrlFileBlob(active.filePath)
                    : active.fileId != null
                        ? await fetchPlatformFileBlob(String(active.fileId))
                        : null;
                if (cancelled || !blob)
                    return;
                const kind = resolveFilePreviewKind(active.fileName || item.fileName || item.title || '', undefined, blob.type);
                if (!cancelled && (kind === 'docx' || kind === 'spreadsheet')) {
                    setSniffedKind(kind);
                }
                else if (!cancelled && isOfficeEditableMaterialType(item.materialType)) {
                    setSniffedKind(item.materialType === 'roster' || item.materialType === 'summary' ? 'spreadsheet' : 'docx');
                }
            }
            catch {
                if (!cancelled && isOfficeEditableMaterialType(item.materialType)) {
                    setSniffedKind(item.materialType === 'roster' || item.materialType === 'summary' ? 'spreadsheet' : 'docx');
                }
            }
        };
        void sniff();
        return () => {
            cancelled = true;
        };
    }, [
        item?.id,
        item?.hasFile,
        item?.fileName,
        item?.title,
        item?.materialType,
        item?.uploadTemplateFileName,
        active?.fileId,
        active?.filePath,
        active?.fileName,
        multiFile,
    ]);
    if (!item) {
        return (_jsx("div", { className: "material-workspace-empty", children: _jsx(Empty, { description: "\u8BF7\u4ECE\u5DE6\u4FA7\u9009\u62E9\u6750\u6599\u67E5\u770B\u6216\u7F16\u8F91" }) }));
    }
    const nameBundle = `${active?.fileName || ''} ${item.fileName || ''} ${item.uploadTemplateFileName || ''} ${active?.filePath || ''}`;
    const spreadsheet = sniffedKind === 'spreadsheet'
        || (sniffedKind == null
            && (isSpreadsheet(active?.fileName, active?.filePath)
                || isSpreadsheet(item.fileName, item.filePath)
                || /\.xlsx?/i.test(nameBundle)));
    // 申请表：扩展名缺失时也先按 Word 露出编辑（预览已能用 MIME 渲 docx）
    const wordDoc = sniffedKind === 'docx'
        || (sniffedKind == null
            && (isWordDoc(active?.fileName, active?.filePath)
                || isWordDoc(item.fileName, item.filePath)
                || /\.docx?/i.test(nameBundle)))
        || (sniffedKind == null
            && item.hasFile
            && item.materialType === 'application_form'
            && !isSpreadsheet(nameBundle));
    const canEditSpreadsheet = !!active && !multiFile && spreadsheet && item.hasFile && !!onReplaceFile;
    const canEditWord = !!active && !multiFile && wordDoc && item.hasFile;
    const canEdit = item.hasFile
        && !multiFile
        && !!active
        && (canEditSpreadsheet
            || canEditWord
            || (isOfficeEditableMaterialType(item.materialType) && item.hasFile));
    const downloadTarget = active || (!multiFile ? targets[0] : null);
    const handleSaveSpreadsheetCells = async (cells) => {
        setSavingCells(true);
        try {
            await trainApi.patchSubsidyMaterialCells(item.id, cells);
            message.success('已保存（保留原表边框格式）');
            setEditing(false);
            onOfficeSaved?.();
        }
        catch (e) {
            message.error(e.message || '保存失败');
            throw e;
        }
        finally {
            setSavingCells(false);
        }
    };
    const enterEditing = () => {
        if (editing) {
            setEditing(false);
            return;
        }
        if (sniffedKind == null && item.materialType === 'application_form' && !spreadsheet) {
            setSniffedKind('docx');
        }
        setEditing(true);
    };
    const showWordEditor = editing && !!active && (canEditWord || (editing && sniffedKind === 'docx'));
    const showSheetEditor = editing && !!active && canEditSpreadsheet;
    return (_jsxs("div", { className: "material-workspace-panel", children: [_jsxs("div", { className: "material-workspace-header", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 5, style: { margin: 0 }, children: item.title }), _jsxs(Space, { size: 8, wrap: true, style: { marginTop: 6 }, children: [_jsx(MaterialStatusTag, { status: item.status }), item.materialTypeLabel ? (_jsx(Typography.Text, { type: "secondary", children: item.materialTypeLabel })) : null, item.versionNo != null ? (_jsxs(Typography.Text, { type: "secondary", children: ["v", item.versionNo] })) : null, multiFile ? (_jsxs(Typography.Text, { type: "secondary", children: [targets.length, " \u4E2A\u6587\u4EF6"] })) : null] })] }), _jsxs(Space, { wrap: true, size: [8, 8], className: "material-workspace-actions", children: [multiFile && active ? (_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: () => setSelectedKey(null), children: "\u8FD4\u56DE\u5217\u8868" })) : null, onFillFromTemplate && !multiFile ? (_jsx(Button, { onClick: onFillFromTemplate, disabled: editing, children: "\u4ECE\u6A21\u677F\u586B\u6570" })) : null, onReplaceFile && !multiFile ? (_jsx(Upload, { showUploadList: false, disabled: editing, beforeUpload: (file) => {
                                    void onReplaceFile(file);
                                    return false;
                                }, children: _jsx(Button, { loading: replaceLoading, disabled: editing, children: "\u66FF\u6362\u4E0A\u4F20" }) })) : null, canEdit ? (_jsx(Button, { type: editing ? 'primary' : 'default', icon: _jsx(EditOutlined, {}), onClick: enterEditing, children: editing ? '退出编辑' : '编辑' })) : null, item.status !== 'confirmed' && item.hasFile && onConfirm && !multiFile ? (_jsx(Button, { type: "primary", loading: confirmLoading, onClick: onConfirm, disabled: editing, children: "\u786E\u8BA4\u672C\u6750\u6599" })) : null, downloadTarget?.filePath ? (_jsx(Button, { disabled: editing, onClick: () => void downloadUrlFile(downloadTarget.filePath, downloadTarget.fileName || item.fileName || 'material-file').catch((e) => {
                                    message.error(e?.message || '下载失败');
                                }), children: "\u4E0B\u8F7D" })) : null] })] }), !item.hasFile && targets.length === 0 ? (_jsx(Empty, { description: "\u6682\u65E0\u6587\u4EF6\uFF1A\u53EF\u7528\u300C\u4ECE\u6A21\u677F\u586B\u6570\u300D\u3001\u300C\u66FF\u6362\u4E0A\u4F20\u300D\uFF0C\u6216\u5148\u300C\u4ECE\u672C\u73ED\u5237\u65B0\u6750\u6599\u300D" })) : multiFile && !active ? (_jsxs("div", { className: "material-file-list", children: [_jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 8 }, children: "\u672C\u6750\u6599\u542B\u591A\u4E2A\u6587\u4EF6\uFF0C\u8BF7\u70B9\u51FB\u67E5\u770B\uFF1A" }), _jsx(List, { size: "small", bordered: true, dataSource: targets, renderItem: (f, idx) => (_jsx(List.Item, { className: "material-file-list-item", onClick: () => setSelectedKey(f.key), actions: [
                                _jsx(Button, { type: "link", size: "small", onClick: () => setSelectedKey(f.key), children: "\u67E5\u770B" }, "view"),
                            ], children: _jsx(List.Item.Meta, { avatar: _jsx(FileOutlined, {}), title: f.fileName || `文件 ${idx + 1}`, description: f.filePath || (f.fileId != null ? `fileId=${f.fileId}` : undefined) }) })) })] })) : active && showSheetEditor ? (_jsx(MaterialSpreadsheetEditor, { fileId: active.fileId, filePath: active.filePath, fileName: active.fileName || item.fileName, onSaveCells: handleSaveSpreadsheetCells, saveLoading: savingCells || !!replaceLoading, editable: true })) : active && showWordEditor ? (wordEditorMode === 'office' && onlyOfficeAvailable ? (_jsxs("div", { children: [_jsx("div", { style: { padding: '8px 12px' }, children: _jsx(Button, { type: "link", size: "small", onClick: () => setWordEditorMode('meta'), children: "\u2190 \u8FD4\u56DE\u5B57\u6BB5\u7F16\u8F91" }) }), _jsx(DocxOfficeEditor, { instanceId: item.id, versionKey: item.currentVersionId ?? item.versionNo, onUnavailable: () => {
                            message.info('OnlyOffice 未启用，已切换回字段编辑');
                            setWordEditorMode('meta');
                            setOnlyOfficeAvailable(false);
                        }, onSaved: () => {
                            onOfficeSaved?.();
                            setEditing(false);
                        } })] })) : (_jsx(DocxMetaEditor, { instanceId: item.id, onlyOfficeAvailable: onlyOfficeAvailable, onCancel: () => setEditing(false), onOpenFullEditor: onlyOfficeAvailable
                    ? () => setWordEditorMode('office')
                    : undefined, onSaved: () => {
                    onOfficeSaved?.();
                    setEditing(false);
                } }))) : active && editing ? (_jsx(Empty, { description: "\u6B63\u5728\u8BC6\u522B\u6587\u4EF6\u7C7B\u578B\u2026\u82E5\u4ECD\u65E0\u6CD5\u7F16\u8F91\uFF0C\u8BF7\u786E\u8BA4\u6587\u4EF6\u4E3A docx/xlsx" })) : active ? (_jsx(MaterialFilePreview, { fileId: active.fileId, filePath: active.filePath, fileName: active.fileName || item.fileName })) : (_jsx(Empty, { description: "\u6682\u65E0\u6587\u4EF6\u53EF\u9884\u89C8" }))] }));
}
