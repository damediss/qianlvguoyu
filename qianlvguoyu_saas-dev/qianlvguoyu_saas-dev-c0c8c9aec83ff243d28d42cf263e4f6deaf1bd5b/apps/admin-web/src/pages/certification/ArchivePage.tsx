import { FolderOpenOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Empty, message, Space, Tabs, Typography } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { archiveBasic, confirmArchiveRecord, getArchiveCompleteCheck } from '../../api/certificationArchive';
import { PermissionGate } from '../../directives/permission';
import type { CertificationArchiveTask } from '../../types/certificationArchive';
import { ArchiveDetail } from './archive/ArchiveDetail';
import { ArchiveImportBatch } from './archive/ArchiveImportBatch';
import { ArchiveWorkbench } from './archive/ArchiveWorkbench';
import { BasicArchive } from './archive/BasicArchive';
import { CandidateMaterialImport } from './archive/CandidateMaterialImport';
import { TaskArchiveMaterials } from './archive/TaskArchiveMaterials';

export function ArchivePage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState<string>();
  const [archiveId, setArchiveId] = useState<string>();
  const [activeTab, setActiveTab] = useState('basic');

  const basicMutation = useMutation({
    mutationFn: (task: CertificationArchiveTask) => archiveBasic(task.taskId),
    onSuccess: (snapshot) => {
      message.success('基础信息已归档');
      setArchiveId(snapshot.archiveId);
      setActiveTab('basic');
      queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
    },
  });

  const confirmMutation = useMutation({
    mutationFn: async () => {
      if (!archiveId) {
        throw new Error('请选择归档记录');
      }
      const check = await getArchiveCompleteCheck(archiveId);
      if (!check.passed) {
        return check;
      }
      return confirmArchiveRecord(archiveId);
    },
    onSuccess: (check) => {
      if (check.passed) {
        message.success('归档已完成');
      } else {
        message.warning(check.items.find((item) => !item.passed)?.message || '归档检查未通过');
      }
      queryClient.invalidateQueries({ queryKey: ['certification-archive-detail', archiveId] });
      queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
    },
  });

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-archive'] });
    queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
  };

  return (
    <PermissionGate code="certification.archive.task.view" fallback={<Empty description="暂无认定归档访问权限" />}>
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div>
            <div className="page-title-line">
              <div className="page-title-icon"><FolderOpenOutlined /></div>
              <div>
                <Typography.Title level={1}>认定归档</Typography.Title>
                <Typography.Text type="secondary">已完成认定任务的基础快照、材料归集和文件访问</Typography.Text>
              </div>
            </div>
            {archiveId && <Typography.Text type="secondary">当前归档记录：{archiveId}</Typography.Text>}
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refreshAll}>刷新</Button>
            <Button type="primary" disabled={!archiveId} loading={confirmMutation.isPending} onClick={() => confirmMutation.mutate()}>
              完成归档
            </Button>
          </Space>
        </div>

        <Card title="认定完成任务" size="small" style={{ marginBottom: 16 }}>
          <ArchiveWorkbench
            keyword={keyword}
            onKeywordChange={setKeyword}
            onArchiveBasic={(task) => basicMutation.mutate(task)}
            onSelectArchive={(id) => {
              setArchiveId(id);
              setActiveTab('detail');
            }}
          />
        </Card>

        <Card size="small">
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              { key: 'basic', label: '基础信息归档', children: <BasicArchive archiveId={archiveId} /> },
              { key: 'materials', label: '认定材料', children: <TaskArchiveMaterials archiveId={archiveId} /> },
              { key: 'import', label: '考生材料导入', children: <CandidateMaterialImport archiveId={archiveId} /> },
              { key: 'batches', label: '导入批次', children: <ArchiveImportBatch archiveId={archiveId} /> },
              { key: 'detail', label: '归档详情', children: <ArchiveDetail archiveId={archiveId} /> },
            ]}
          />
        </Card>
      </div>
    </PermissionGate>
  );
}
