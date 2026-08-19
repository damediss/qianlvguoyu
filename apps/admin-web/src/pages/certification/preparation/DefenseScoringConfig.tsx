import { FileProtectOutlined, MobileOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Empty,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { logout } from '../../../api/auth';
import {
  bindDefenseCandidateReport,
  deleteDefenseCandidateReport,
  getDefenseScoringSetup,
  matchDefenseReports,
  saveDefenseScoringConfig,
} from '../../../api/defenseScoring';
import { fetchPlatformFileBlob, uploadPlatformFile } from '../../../api/files';
import { PermissionGate } from '../../../directives/permission';
import { useAuthStore } from '../../../stores/authStore';
import type { DefenseCandidateRow, DefenseUnmatchedFile } from '../../../types/defenseScoring';

interface Props {
  taskId?: string;
}

export function DefenseScoringConfig({ taskId }: Props) {
  const queryClient = useQueryClient();
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [groupId, setGroupId] = useState<string>();
  const [preview, setPreview] = useState<DefenseCandidateRow>();
  const [unmatched, setUnmatched] = useState<DefenseUnmatchedFile[]>([]);

  const setupQuery = useQuery({
    queryKey: ['defense-scoring-setup', taskId],
    queryFn: () => getDefenseScoringSetup(taskId!),
    enabled: Boolean(taskId),
  });

  const setup = setupQuery.data;
  const uploadedCount = useMemo(
    () => (setup?.candidates || []).filter((row) => row.fileId).length,
    [setup?.candidates],
  );

  useEffect(() => {
    if (setup?.config?.groupId) {
      setGroupId(setup.config.groupId);
    }
  }, [setup?.config?.groupId]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['defense-scoring-setup', taskId] });
  };

  const saveConfigMutation = useMutation({
    mutationFn: (selectedGroupId: string) => saveDefenseScoringConfig(taskId!, selectedGroupId),
    onSuccess: () => {
      message.success('已绑定综合答辩考评组');
      refresh();
    },
  });

  const matchMutation = useMutation({
    mutationFn: async (files: File[]) => {
      const uploaded = [];
      for (const file of files) {
        const stored = await uploadPlatformFile(file);
        uploaded.push({ fileId: String(stored.id), fileName: file.name });
      }
      return matchDefenseReports(taskId!, uploaded);
    },
    onSuccess: (result) => {
      const parts = [`成功匹配 ${result.matched} 份`];
      if (result.replaced) parts.push(`替换 ${result.replaced} 份`);
      if (result.unmatched) parts.push(`未匹配 ${result.unmatched} 份`);
      message.success(parts.join('，'));
      setUnmatched(result.unmatchedFiles || []);
      refresh();
    },
  });

  const bindMutation = useMutation({
    mutationFn: async ({ candidateId, file }: { candidateId: string; file: File }) => {
      const stored = await uploadPlatformFile(file);
      return bindDefenseCandidateReport(taskId!, candidateId, {
        fileId: String(stored.id),
        fileName: file.name,
      });
    },
    onSuccess: () => {
      message.success('已上传业绩评估报告');
      refresh();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (candidateId: string) => deleteDefenseCandidateReport(taskId!, candidateId),
    onSuccess: () => {
      message.success('已删除业绩评估报告');
      refresh();
    },
  });

  if (!taskId) {
    return <Typography.Text type="secondary">请选择认定任务。</Typography.Text>;
  }

  const columns: ColumnsType<DefenseCandidateRow> = [
    { title: '考生', dataIndex: 'candidateName', width: 110, fixed: 'left' },
    { title: '证件号', dataIndex: 'maskedIdCardNo', width: 180, render: (v, row) => v || row.idCardNo || '-' },
    { title: '工种', dataIndex: 'workTypeName', width: 120, render: (v) => v || '-' },
    { title: '等级', dataIndex: 'levelName', width: 100, render: (v) => v || '-' },
    { title: '单位', dataIndex: 'orgName', width: 160, ellipsis: true, render: (v) => v || '-' },
    {
      title: '业绩评估报告',
      width: 260,
      render: (_, row) => (row.fileId
        ? <Tag color="success">{row.fileName || '已上传 PDF'}</Tag>
        : <Tag>未上传</Tag>),
    },
    {
      title: '操作',
      width: 240,
      render: (_, row) => (
        <Space wrap>
          <PermissionGate code="certification.preparation.task.view">
            <Upload
              accept=".pdf,application/pdf"
              showUploadList={false}
              beforeUpload={(file) => {
                if (!isPdfFile(file)) {
                  message.error('仅支持 PDF 文件');
                  return false;
                }
                bindMutation.mutate({ candidateId: row.candidateId, file });
                return false;
              }}
            >
              <Button size="small" icon={<UploadOutlined />} loading={bindMutation.isPending}>
                {row.fileId ? '替换' : '上传'}
              </Button>
            </Upload>
          </PermissionGate>
          {row.fileId ? (
            <Button size="small" onClick={() => setPreview(row)}>预览</Button>
          ) : null}
          {row.fileId ? (
            <PermissionGate code="certification.preparation.task.view">
              <Button size="small" danger onClick={() => deleteMutation.mutate(row.candidateId)}>
                删除
              </Button>
            </PermissionGate>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <ProCard
        title={(
          <Space>
            <FileProtectOutlined />
            综合答辩配置
          </Space>
        )}
        extra={(
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <Button
              icon={<MobileOutlined />}
              onClick={async () => {
                try {
                  await logout();
                } catch {
                  // 切换入口时仍清理登录态
                } finally {
                  clearAuth();
                  window.location.assign('/assessor-defense');
                }
              }}
            >
              打开综合答辩端
            </Button>
          </Space>
        )}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            选择本任务综合答辩考评组后，按考生匹配上传业绩评估报告 PDF。考评员使用考评员信息中创建的账号，从综合答辩端竖屏阅览报告，并完成论文答辩评分与业绩评估报告评分。
          </Typography.Text>
          <Space wrap>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="请选择考评组"
              value={groupId}
              loading={setupQuery.isFetching}
              style={{ minWidth: 320 }}
              options={(setup?.groups || []).map((group) => ({
                value: group.id,
                disabled: group.memberCount < 1,
                label: `${group.groupName || '未命名考评组'}（${group.memberCount}人${group.leaderAssessorName ? ` · 组长${group.leaderAssessorName}` : ''}）`,
              }))}
              onChange={setGroupId}
            />
            <PermissionGate code="certification.preparation.task.view">
              <Button
                type="primary"
                loading={saveConfigMutation.isPending}
                onClick={() => {
                  if (!groupId) {
                    message.warning('请选择考评组');
                    return;
                  }
                  saveConfigMutation.mutate(groupId);
                }}
              >
                保存考评组
              </Button>
            </PermissionGate>
            {setup?.config?.groupName ? <Tag color="processing">当前：{setup.config.groupName}</Tag> : null}
          </Space>
        </Space>
      </ProCard>

      <ProCard
        title={`业绩评估报告（已匹配 ${uploadedCount}/${setup?.candidates.length || 0}）`}
        extra={(
          <PermissionGate code="certification.preparation.task.view">
            <Upload
              accept=".pdf,application/pdf"
              multiple
              showUploadList={false}
              beforeUpload={(file, fileList) => {
                if (file !== fileList[fileList.length - 1]) {
                  return false;
                }
                const pdfs = fileList.filter((item) => isPdfFile(item));
                if (pdfs.length !== fileList.length) {
                  message.warning('已忽略非 PDF 文件');
                }
                if (pdfs.length === 0) {
                  message.error('请选择 PDF 文件');
                  return false;
                }
                matchMutation.mutate(pdfs);
                return false;
              }}
            >
              <Button icon={<UploadOutlined />} loading={matchMutation.isPending}>
                按考生匹配上传
              </Button>
            </Upload>
          </PermissionGate>
        )}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="批量上传时，文件名需包含考生身份证号、准考证号或姓名，系统会自动匹配到对应考生。"
          />
          {unmatched.length > 0 ? (
            <Alert
              type="warning"
              showIcon
              message="以下文件未能匹配"
              description={unmatched.map((item) => `${item.fileName || '未命名文件'}：${item.reason}`).join('；')}
            />
          ) : null}
          <Table
            rowKey="candidateId"
            columns={columns}
            dataSource={setup?.candidates || []}
            loading={setupQuery.isFetching}
            pagination={{ pageSize: 10 }}
            scroll={{ x: 1100 }}
            locale={{ emptyText: '暂无考生，请先完成考生导入' }}
          />
        </Space>
      </ProCard>

      <Modal
        title={preview ? `${preview.candidateName} · 业绩评估报告` : '预览'}
        open={Boolean(preview)}
        onCancel={() => setPreview(undefined)}
        footer={null}
        width={920}
        destroyOnHidden
      >
        {preview?.fileId ? <DefensePdfFrame fileId={preview.fileId} /> : <Empty description="暂无文件" />}
      </Modal>
    </Space>
  );
}

function isPdfFile(file: File) {
  return file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
}

export function DefensePdfFrame({ fileId }: { fileId: string }) {
  const query = useQuery({
    queryKey: ['defense-pdf', fileId],
    queryFn: async () => {
      const blob = await fetchPlatformFileBlob(fileId);
      return URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
    },
    enabled: Boolean(fileId),
  });

  if (query.isLoading) {
    return <Spin style={{ display: 'block', margin: '48px auto' }} />;
  }
  if (!query.data) {
    return <Empty description="报告加载失败" />;
  }
  return (
    <iframe
      title="业绩评估报告"
      src={`${query.data}#toolbar=1&navpanes=0`}
      style={{ width: '100%', height: '70vh', border: 0, background: '#525659' }}
    />
  );
}
