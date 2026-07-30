import {
  DatabaseOutlined,
  ExperimentOutlined,
  EyeOutlined,
  FilePptOutlined,
  ReadOutlined,
  ReloadOutlined,
  SafetyOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { Button, Empty, Input, Modal, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { pageProjectResources } from '../../api/industrial';
import type { ProjectResource, ProjectResourceType } from '../../types/industrial';

const configs = {
  course: {
    title: '课件列表',
    desc: '管理培训项目课程资源列表，支持课件文件预览。',
    nameLabel: '课件名称',
    sourceLabel: '来源',
    sourceKey: 'source',
    permission: 'industrial:project-course:view',
    preview: true,
    icon: <FilePptOutlined />,
    sourceWidth: 320,
    bordered: true,
  },
  teach: {
    title: '教材列表',
    desc: '管理培训项目教材资源列表，预览逻辑与课件保持一致。',
    nameLabel: '教材名称',
    sourceLabel: '来源',
    sourceKey: 'source',
    permission: 'industrial:project-teach:view',
    preview: true,
    icon: <ReadOutlined />,
    sourceWidth: 320,
    bordered: false,
  },
  exam: {
    title: '题库列表',
    desc: '查看培训项目考核题库资源列表。',
    nameLabel: '题库名称',
    sourceLabel: '来源',
    sourceKey: 'source',
    permission: 'industrial:project-exam:view',
    preview: false,
    icon: <DatabaseOutlined />,
    sourceWidth: 120,
    bordered: true,
  },
  train: {
    title: '实训中心',
    desc: '查看实训中心数据管理列表。',
    nameLabel: '名称',
    sourceLabel: '企业',
    sourceKey: 'company',
    permission: 'industrial:project-train:view',
    preview: false,
    icon: <ExperimentOutlined />,
    sourceWidth: 320,
    bordered: true,
  },
  criterion: {
    title: '标准',
    desc: '查看开发工程标准资源列表。',
    nameLabel: '名称',
    sourceLabel: '企业名称',
    sourceKey: 'company',
    permission: 'industrial:project-criterion:view',
    preview: false,
    icon: <SafetyOutlined />,
    sourceWidth: 320,
    bordered: true,
  },
} satisfies Record<ProjectResourceType, {
  title: string;
  desc: string;
  nameLabel: string;
  sourceLabel: string;
  sourceKey: 'source' | 'company';
  permission: string;
  preview: boolean;
  icon: JSX.Element;
  sourceWidth: number;
  bordered: boolean;
}>;

interface ProjectResourcePageProps {
  type: ProjectResourceType;
}

export function ProjectResourcePage({ type }: ProjectResourcePageProps) {
  const config = configs[type];
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '', state: '' });
  const [previewRecord, setPreviewRecord] = useState<ProjectResource | null>(null);

  const listQuery = useQuery({
    queryKey: ['industrial-project-resource', type, query],
    queryFn: () => pageProjectResources(type, query),
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['industrial-project-resource', type] });
  }, [queryClient, type]);

  const columns = useMemo(() => {
    const result: TableColumnsType<ProjectResource> = [
      { title: '创建日期', dataIndex: 'date', width: 150 },
      { title: config.nameLabel, dataIndex: 'name', minWidth: 120, ellipsis: true },
      {
        title: '状态',
        dataIndex: 'state',
        width: 120,
        render: (value: string) => <Tag color={value === '通过' ? 'success' : 'processing'}>{value}</Tag>,
      },
      {
        title: config.sourceLabel,
        dataIndex: config.sourceKey,
        width: config.sourceWidth,
        ellipsis: true,
      },
    ];

    if (config.preview) {
      result.push({
        title: '操作',
        width: 120,
        fixed: 'right',
        render: (_: unknown, record: ProjectResource) => (
          <Button type="link" icon={<EyeOutlined />} onClick={() => setPreviewRecord(record)}>
            预览
          </Button>
        ),
      });
    }

    return result;
  }, [config]);

  const items = listQuery.data?.items || [];
  const previewType = previewRecord?.file_type || previewRecord?.fileType;
  const previewUrl = previewRecord?.preview || '';
  const officePreviewUrl = previewUrl
    ? `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(previewUrl)}`
    : '';

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon">{config.icon}</span>
            <Typography.Title level={1}>{config.title}</Typography.Title>
          </div>
          <Typography.Text>{config.desc}</Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px 24px' }}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder={`搜索${config.nameLabel}、状态、来源`}
            allowClear
            style={{ width: 280 }}
            value={query.keyword}
            onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          <Input
            placeholder="状态"
            allowClear
            style={{ width: 160 }}
            value={query.state}
            onChange={(event) => setQuery((prev) => ({ ...prev, state: event.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          <Button icon={<SearchOutlined />} onClick={refresh}>搜索</Button>
        </Space>

        <Table
          rowKey="id"
          size="middle"
          bordered={config.bordered}
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: listQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (count) => `共 ${count} 条`,
            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <Modal
        title="预览"
        open={!!previewRecord}
        onCancel={() => setPreviewRecord(null)}
        footer={null}
        width="60%"
        destroyOnClose
      >
        <div style={{ height: 600, border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          {previewUrl ? (
            previewType === 'pdf' ? (
              <iframe title="文件预览" src={previewUrl} style={{ width: '100%', height: '100%', border: 0 }} />
            ) : previewType === 'docx' || previewType === 'pptx' ? (
              // Office Online fallback: https://view.officeapps.live.com/op/view.aspx?src={preview}
              <iframe title="文件预览" src={officePreviewUrl} style={{ width: '100%', height: '100%', border: 0 }} />
            ) : (
              <Empty description="暂不支持该文件类型预览" style={{ marginTop: 180 }} />
            )
          ) : (
            <Empty description="暂无预览文件" style={{ marginTop: 180 }} />
          )}
        </div>
      </Modal>
    </div>
  );
}
