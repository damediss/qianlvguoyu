import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 工作台行内"入库 / 导出"操作：入库带任务模式选择与"低于配额二次确认"，导出直接下载 xlsx。
 * 以 hook 形式暴露给总览表行操作，Modal 由调用方渲染（modal 节点随 hook 返回）。
 */
import { useState } from 'react';
import { message, Modal, Radio, Typography } from 'antd';
import { exportDrafts, publishDrafts } from '../theoryGenApi';
export const usePublishExport = (onChanged) => {
    const [publishingBatchId, setPublishingBatchId] = useState();
    const [exportingBatchId, setExportingBatchId] = useState();
    const [publishTarget, setPublishTarget] = useState();
    const [mode, setMode] = useState('create');
    const doPublish = async (batchId, taskMode, confirmedBelowQuota) => {
        setPublishingBatchId(batchId);
        try {
            const r = await publishDrafts(batchId, taskMode, undefined, undefined, confirmedBelowQuota);
            message.success(`入库成功：客观 ${r.objectiveCount} 题、主观 ${r.subjectiveCount} 题`);
            onChanged();
        }
        catch (e) {
            const msg = e instanceof Error ? e.message : '';
            if (msg.includes('需二次确认')) {
                Modal.confirm({
                    title: '题目数量未达本批次计划',
                    content: `${msg.replace('【需二次确认】', '')}。确认要继续入库吗？继续后本次操作会被记录备查。`,
                    onOk: () => doPublish(batchId, taskMode, true),
                });
            }
        }
        finally {
            setPublishingBatchId(undefined);
        }
    };
    const exportXlsx = async (batchId) => {
        setExportingBatchId(batchId);
        try {
            const blob = await exportDrafts(batchId);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `theory-questions-${batchId}.xlsx`;
            a.click();
            URL.revokeObjectURL(url);
            message.success('试题表已导出');
        }
        catch {
            // 错误提示由拦截器统一处理
        }
        finally {
            setExportingBatchId(undefined);
        }
    };
    const modal = (_jsxs(Modal, { open: !!publishTarget, title: `入库到题库开发任务（批次 …${(publishTarget ?? '').slice(-8)}）`, okText: "\u786E\u8BA4\u5165\u5E93", onCancel: () => setPublishTarget(undefined), onOk: () => {
            const id = publishTarget;
            setPublishTarget(undefined);
            if (id) {
                void doPublish(id, mode);
            }
        }, children: [_jsx(Typography.Paragraph, { type: "secondary", style: { fontSize: 12 }, children: "\u53EA\u6709\u300C\u5DF2\u901A\u8FC7\u5BA1\u6838\u300D\u7684\u9898\u76EE\u4F1A\u5199\u5165\u547D\u9898\u4EFB\u52A1\uFF1B\u9A73\u56DE\u548C\u672A\u5BA1\u6838\u7684\u4E0D\u4F1A\u5199\u5165\u3002 \u82E5\u672C\u6279\u6B21\u8BA1\u5212\u9898\u76EE\u672A\u5168\u90E8\u901A\u8FC7\uFF0C\u5165\u5E93\u524D\u4F1A\u518D\u6B21\u4E0E\u60A8\u786E\u8BA4\u3002" }), _jsxs(Radio.Group, { value: mode, onChange: (e) => setMode(e.target.value), children: [_jsx(Radio.Button, { value: "create", children: "\u65B0\u5EFA\u547D\u9898\u4EFB\u52A1" }), _jsx(Radio.Button, { value: "append", children: "\u8FFD\u52A0\u73B0\u6709\u4EFB\u52A1" })] })] }));
    return {
        openPublish: (batchId) => setPublishTarget(batchId),
        publish: async (batchId, taskMode, confirmed) => doPublish(batchId, taskMode, confirmed),
        exportXlsx,
        publishingBatchId,
        exportingBatchId,
        modal,
    };
};
