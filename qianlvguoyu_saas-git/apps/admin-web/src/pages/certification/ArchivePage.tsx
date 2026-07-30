import { FolderOpenOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Empty, message, Modal, Space, Tabs, Typography } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { archiveBasic } from '../../api/certificationArchive';
import { PermissionGate } from '../../directives/permission';
import type { CertificationArchiveTask } from '../../types/certificationArchive';
import { ArchiveDetail } from './archive/ArchiveDetail';
import { ArchiveWorkbench } from './archive/ArchiveWorkbench';
import { BasicArchive } from './archive/BasicArchive';
import { CandidateMaterialImport } from './archive/CandidateMaterialImport';
import { TaskArchiveMaterials } from './archive/TaskArchiveMaterials';

export function ArchivePage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState<string>();
  const [archiveId, setArchiveId] = useState<string>();
  const [activeTab, setActiveTab] = useState('basic');
  const [detailOpen, setDetailOpen] = useState(false);

  const basicMutation = useMutation({
    mutationFn: (task: CertificationArchiveTask) => archiveBasic(task.taskId),
    onSuccess: (snapshot) => {
      message.success('基础信息已归档');
      setArchiveId(snapshot.archiveId);
      setActiveTab('basic');
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
          </Space>
        </div>

        <Card title="任务栏" size="small" style={{ marginBottom: 16 }}>
          <ArchiveWorkbench
            keyword={keyword}
            onKeywordChange={setKeyword}
            onArchiveBasic={(task) => basicMutation.mutate(task)}
            onSelectArchive={(id) => {
              setArchiveId(id);
              setDetailOpen(true);
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
            ]}
          />
        </Card>
        <Modal
          title="认定归档详情"
          open={detailOpen}
          width={1120}
          footer={null}
          destroyOnHidden
          onCancel={() => setDetailOpen(false)}
        >
          <ArchiveDetail
            archiveId={archiveId}
            onCompleted={() => {
              setDetailOpen(false);
              setArchiveId(undefined);
              setActiveTab('basic');
              refreshAll();
            }}
          />
        </Modal>
      </div>
    </PermissionGate>
  );
}
