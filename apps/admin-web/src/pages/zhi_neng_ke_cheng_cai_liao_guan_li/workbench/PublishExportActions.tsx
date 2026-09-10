/**
 * 工作台行内"入库 / 导出"操作：入库带任务模式选择与"低于配额二次确认"，导出直接下载 xlsx。
 * 以 hook 形式暴露给总览表行操作，Modal 由调用方渲染（modal 节点随 hook 返回）。
 */
import React, { useState } from 'react';
import { message, Modal, Radio, Typography } from 'antd';
import { exportDrafts, publishDrafts } from '../theoryGenApi';

export interface PublishExportApi {
  /** 打开入库确认 Modal（选择新建/追加任务） */
  openPublish: (batchId: string) => void;
  /** 入库：mode=create 新建命题任务 / append 追加现有任务 */
  publish: (batchId: string, mode: 'create' | 'append', confirmedBelowQuota?: boolean) => Promise<void>;
  exportXlsx: (batchId: string) => Promise<void>;
  publishingBatchId?: string;
  exportingBatchId?: string;
  /** 入库确认 Modal（任务模式选择），由调用方挂在组件树里 */
  modal: React.ReactNode;
}

export const usePublishExport = (onChanged: () => void): PublishExportApi => {
  const [publishingBatchId, setPublishingBatchId] = useState<string | undefined>();
  const [exportingBatchId, setExportingBatchId] = useState<string | undefined>();
  const [publishTarget, setPublishTarget] = useState<string | undefined>();
  const [mode, setMode] = useState<'create' | 'append'>('create');

  const doPublish = async (batchId: string, taskMode: 'create' | 'append',
      confirmedBelowQuota?: boolean) => {
    setPublishingBatchId(batchId);
    try {
      const r = await publishDrafts(batchId, taskMode, undefined, undefined, confirmedBelowQuota);
      message.success(`入库成功：客观 ${r.objectiveCount} 题、主观 ${r.subjectiveCount} 题`);
      onChanged();
    } catch (e) {
      const msg = e instanceof Error ? e.message : '';
      if (msg.includes('需二次确认')) {
        Modal.confirm({
          title: '题目数量未达本批次计划',
          content: `${msg.replace('【需二次确认】', '')}。确认要继续入库吗？继续后本次操作会被记录备查。`,
          onOk: () => doPublish(batchId, taskMode, true),
        });
      }
    } finally {
      setPublishingBatchId(undefined);
    }
  };

  const exportXlsx = async (batchId: string) => {
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
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      setExportingBatchId(undefined);
    }
  };

  const modal = (
    <Modal
      open={!!publishTarget}
      title={`入库到题库开发任务（批次 …${(publishTarget ?? '').slice(-8)}）`}
      okText="确认入库"
      onCancel={() => setPublishTarget(undefined)}
      onOk={() => {
        const id = publishTarget;
        setPublishTarget(undefined);
        if (id) {
          void doPublish(id, mode);
        }
      }}
    >
      <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
        只有「已通过审核」的题目会写入命题任务；驳回和未审核的不会写入。
        若本批次计划题目未全部通过，入库前会再次与您确认。
      </Typography.Paragraph>
      <Radio.Group value={mode} onChange={(e) => setMode(e.target.value)}>
        <Radio.Button value="create">新建命题任务</Radio.Button>
        <Radio.Button value="append">追加现有任务</Radio.Button>
      </Radio.Group>
    </Modal>
  );

  return {
    openPublish: (batchId) => setPublishTarget(batchId),
    publish: async (batchId, taskMode, confirmed) => doPublish(batchId, taskMode, confirmed),
    exportXlsx,
    publishingBatchId,
    exportingBatchId,
    modal,
  };
};
