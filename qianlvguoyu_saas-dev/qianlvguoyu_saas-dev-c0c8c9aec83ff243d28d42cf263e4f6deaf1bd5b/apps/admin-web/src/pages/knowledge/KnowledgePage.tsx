import {
  ApartmentOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  FileProtectOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  LockOutlined,
  PlusOutlined,
  ReloadOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd';
import type { UploadFile, UploadProps } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import {
  createAnswerDraft,
  createSpace,
  deleteSpace,
  ingestDocument,
  pageDocuments,
  pageSpaces,
  searchKnowledge,
  uploadFile,
} from '../../api/knowledge';
import type { KnowledgeAnswerDraft, KnowledgeDocument, KnowledgeSearchResult, KnowledgeSpace } from '../../types/knowledge';
import { useAuthStore } from '../../stores/authStore';

const scopeOptions = [
  { label: '企业知识库', value: 'enterprise' },
  { label: '部门知识库', value: 'department' },
  { label: '课程知识库', value: 'course' },
];

const roleOptions = [
  { label: '平台管理员', value: 'PLATFORM_ADMIN' },
  { label: '培训负责人', value: 'TRAINING_MANAGER' },
  { label: '课程人员', value: 'COURSE_STAFF' },
  { label: '企业管理员', value: 'ENTERPRISE_ADMIN' },
];

const categoryOptions = [
  { label: '制度流程', value: 'policy' },
  { label: '课程资料', value: 'course' },
  { label: '合同资料', value: 'contract' },
  { label: '项目资料', value: 'project' },
  { label: '客户资料', value: 'customer' },
];

function scopeLabel(value?: string) {
  return scopeOptions.find((item) => item.value === value)?.label || value || '-';
}

function categoryLabel(value?: string) {
  return categoryOptions.find((item) => item.value === value)?.label || value || '-';
}

function roleLabel(value?: string) {
  return roleOptions.find((item) => item.value === value)?.label || value || value;
}

function statusTag(status?: string) {
  if (status === 'enabled' || status === 'indexed') {
    return <Tag color="success">{status === 'enabled' ? '已启用' : '已索引'}</Tag>;
  }
  if (status === 'failed') {
    return <Tag color="error">失败</Tag>;
  }
  return <Tag color="processing">{status || '-'}</Tag>;
}

export function KnowledgePage() {
  const tenantId = useAuthStore((state) => state.tenantId);
  const token = useAuthStore((state) => state.token);
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('spaces');
  const [keyword, setKeyword] = useState('');
  const [selectedSpace, setSelectedSpace] = useState<KnowledgeSpace>();
  const [spaceOpen, setSpaceOpen] = useState(false);
  const [ingestOpen, setIngestOpen] = useState(false);
  const [uploadedFileId, setUploadedFileId] = useState<string>();
  const [uploadList, setUploadList] = useState<UploadFile[]>([]);
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResult[]>([]);
  const [answerDraft, setAnswerDraft] = useState<KnowledgeAnswerDraft>();
  const [spaceForm] = Form.useForm();
  const [ingestForm] = Form.useForm();
  const [searchForm] = Form.useForm();

  const spacesQuery = useQuery({
    queryKey: ['knowledge-spaces', keyword, tenantId],
    queryFn: () => pageSpaces({ page: 1, pageSize: 50, keyword: keyword || undefined }),
    enabled: Boolean(token && tenantId),
  });

  const spaces = spacesQuery.data?.items || [];
  const currentSpace = selectedSpace || spaces[0];

  const documentsQuery = useQuery({
    queryKey: ['knowledge-documents', currentSpace?.id],
    queryFn: () => pageDocuments(currentSpace!.id, { page: 1, pageSize: 20 }),
    enabled: Boolean(tenantId && currentSpace?.id),
  });

  const createSpaceMutation = useMutation({
    mutationFn: createSpace,
    onSuccess: (space) => {
      message.success('知识库已创建');
      setSelectedSpace(space);
      setSpaceOpen(false);
      spaceForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['knowledge-spaces'] });
    },
  });

  const deleteSpaceMutation = useMutation({
    mutationFn: deleteSpace,
    onSuccess: () => {
      message.success('知识库已删除');
      setSelectedSpace(undefined);
      queryClient.invalidateQueries({ queryKey: ['knowledge-spaces'] });
    },
  });

  const ingestMutation = useMutation({
    mutationFn: ({ spaceId, payload }: { spaceId: number; payload: Parameters<typeof ingestDocument>[1] }) =>
      ingestDocument(spaceId, payload),
    onSuccess: (document) => {
      message.success(document.status === 'indexed' ? '资料已入库' : '资料入库失败，请查看错误信息');
      setIngestOpen(false);
      setUploadedFileId(undefined);
      setUploadList([]);
      ingestForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] });
    },
  });

  const searchMutation = useMutation({
    mutationFn: searchKnowledge,
    onSuccess: setSearchResults,
  });

  const answerMutation = useMutation({
    mutationFn: createAnswerDraft,
    onSuccess: setAnswerDraft,
  });

  const uploadProps: UploadProps = {
    fileList: uploadList,
    maxCount: 1,
    beforeUpload: async (file) => {
      const uploaded = await uploadFile(file);
      setUploadedFileId(uploaded.id);
      setUploadList([{ uid: String(uploaded.id), name: uploaded.originalName, status: 'done' }]);
      ingestForm.setFieldsValue({ title: uploaded.originalName });
      message.success('文件已上传');
      return Upload.LIST_IGNORE;
    },
    onRemove: () => {
      setUploadedFileId(undefined);
      setUploadList([]);
    },
  };

  const documentRows = documentsQuery.data?.items || [];
  const indexedCount = useMemo(() => documentRows.filter((doc) => doc.indexStatus === 'indexed').length, [documentRows]);
  const sensitiveCount = useMemo(
    () => documentRows.filter((doc) => doc.accessScope === 'restricted').length,
    [documentRows],
  );
  const roleCount = currentSpace?.visibleRoleCodes.length || 0;

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><DatabaseOutlined /></span>
            <Typography.Title level={1}>企业知识库</Typography.Title>
          </div>
          <Typography.Text>
            统一 RAG 底座，按租户、空间和文档级 ACL 管理资料入库、检索引用与 AI 草稿。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['knowledge-spaces'] })}>
            刷新
          </Button>
          <Button icon={<CloudUploadOutlined />} disabled={!currentSpace} onClick={() => setIngestOpen(true)}>
            上传资料
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setSpaceOpen(true)}>
            新建知识库
          </Button>
        </Space>
      </section>

      <div className="knowledge-overview">
        <ProCard className="overview-card" bordered>
          <Statistic title="知识库空间" value={spaces.length} prefix={<ApartmentOutlined />} />
        </ProCard>
        <ProCard className="overview-card" bordered>
          <Statistic title="当前资料" value={documentRows.length} prefix={<FileTextOutlined />} />
        </ProCard>
        <ProCard className="overview-card" bordered>
          <Statistic title="已索引资料" value={indexedCount} prefix={<CheckCircleOutlined />} />
        </ProCard>
        <ProCard className="overview-card" bordered>
          <Statistic title="受控访问" value={sensitiveCount} suffix={`/ ${roleCount} 角色`} prefix={<LockOutlined />} />
        </ProCard>
      </div>

      <div className="knowledge-grid">
        <main className="knowledge-main">
          <ProCard
            bordered
            className="work-section pro-work-section"
            title="知识库工作台"
            extra={
              <Input
                className="section-search"
                prefix={<SearchOutlined />}
                placeholder="搜索知识库名称或编码"
                allowClear
                onChange={(event) => setKeyword(event.target.value)}
              />
            }
          >
            <Tabs
              className="work-tabs"
              activeKey={activeTab}
              onChange={setActiveTab}
              items={[
                { key: 'spaces', label: '知识库空间' },
                { key: 'ingest', label: '资料入库' },
                { key: 'search', label: '检索问答' },
              ]}
            />
            {activeTab === 'spaces' && (
              <Table<KnowledgeSpace>
                rowKey="id"
                size="middle"
                loading={spacesQuery.isLoading}
                dataSource={spaces}
                pagination={false}
                onRow={(record) => ({
                  onClick: () => setSelectedSpace(record),
                })}
                rowClassName={(record) => (record.id === currentSpace?.id ? 'selected-row' : '')}
                columns={[
                  {
                    title: '知识库',
                    dataIndex: 'name',
                    render: (text, record) => (
                      <div className="name-cell">
                        <span className="name-cell-icon"><DatabaseOutlined /></span>
                        <span>
                          <Typography.Text strong>{text}</Typography.Text>
                          <Typography.Text type="secondary">{record.code}</Typography.Text>
                        </span>
                      </div>
                    ),
                  },
                  { title: '空间类型', dataIndex: 'scopeType', width: 120, render: scopeLabel },
                  {
                    title: '可见角色',
                    dataIndex: 'visibleRoleCodes',
                    render: (roles: string[]) => (
                      <Space size={[4, 4]} wrap>
                        {roles?.map((role) => <Tag key={role}>{roleLabel(role)}</Tag>)}
                      </Space>
                    ),
                  },
                  { title: '状态', dataIndex: 'status', width: 104, render: statusTag },
                  {
                    title: '更新时间',
                    dataIndex: 'updatedAt',
                    width: 156,
                    render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm'),
                  },
                  {
                    title: '操作',
                    width: 90,
                    render: (_, record) => (
                      <Button
                        danger
                        type="link"
                        size="small"
                        onClick={(event) => {
                          event.stopPropagation();
                          Modal.confirm({
                            title: '删除知识库',
                            content: `确认删除 ${record.name}？`,
                            onOk: () => deleteSpaceMutation.mutate(record.id),
                          });
                        }}
                      >
                        删除
                      </Button>
                    ),
                  },
                ]}
              />
            )}

            {activeTab === 'ingest' && (
              <>
                <div className="section-title-row">
                  <div>
                    <Typography.Title level={3}>资料入库</Typography.Title>
                    <Typography.Text type="secondary">
                      上传后解析、切片并建立索引任务，敏感资料必须限制文档可见范围。
                    </Typography.Text>
                  </div>
                  <Button type="primary" icon={<CloudUploadOutlined />} disabled={!currentSpace} onClick={() => setIngestOpen(true)}>
                    上传到当前知识库
                  </Button>
                </div>
                <Table<KnowledgeDocument>
                  rowKey="id"
                  size="middle"
                  loading={documentsQuery.isLoading}
                  dataSource={documentRows}
                  pagination={false}
                  locale={{ emptyText: <Empty description="当前知识库还没有资料" /> }}
                  columns={[
                    {
                      title: '资料',
                      dataIndex: 'title',
                      render: (text, record) => (
                        <div className="name-cell">
                          <span className="name-cell-icon file"><FileProtectOutlined /></span>
                          <span>
                            <Typography.Text strong>{text}</Typography.Text>
                            <Typography.Text type="secondary">fileId {record.fileId}</Typography.Text>
                          </span>
                        </div>
                      ),
                    },
                    { title: '分类', dataIndex: 'categoryCode', width: 110, render: categoryLabel },
                    {
                      title: '权限',
                      dataIndex: 'accessScope',
                      width: 130,
                      render: (value, record) => value === 'restricted' ? (
                        <Tag icon={<LockOutlined />} color="error">{record.sensitiveLevel}</Tag>
                      ) : (
                        <Tag color="processing">继承空间</Tag>
                      ),
                    },
                    { title: '切片数', dataIndex: 'chunkCount', width: 90 },
                    { title: '索引', dataIndex: 'indexStatus', width: 110, render: statusTag },
                    {
                      title: '更新时间',
                      dataIndex: 'updatedAt',
                      width: 156,
                      render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm'),
                    },
                  ]}
                />
              </>
            )}

            {activeTab === 'search' && (
              <section className="search-workspace">
                <Form
                  form={searchForm}
                  layout="vertical"
                  initialValues={{ topK: 5, query: '合同金额' }}
                  onFinish={(values) => {
                    const payload = {
                      query: values.query,
                      question: values.query,
                      spaceIds: currentSpace ? [currentSpace.id] : undefined,
                      topK: values.topK,
                    };
                    searchMutation.mutate(payload);
                    answerMutation.mutate(payload);
                  }}
                >
                  <div className="search-command">
                    <Form.Item name="query" label="检索问题" rules={[{ required: true }]} className="search-input-item">
                      <Input prefix={<FileSearchOutlined />} placeholder="输入要查询的问题，例如：合同金额是多少？" />
                    </Form.Item>
                    <Form.Item name="topK" label="片段数">
                      <InputNumber min={1} max={20} />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" loading={searchMutation.isPending || answerMutation.isPending}>
                      检索并生成草稿
                    </Button>
                  </div>
                </Form>
                <div className="search-result-grid">
                  <div className="result-list">
                    <Typography.Title level={4}>引用片段</Typography.Title>
                    {searchResults.length === 0 ? (
                      <Empty description="暂无检索结果" />
                    ) : searchResults.map((item) => (
                      <article className="citation-row" key={item.chunkId}>
                        <div className="citation-score">{item.score.toFixed(0)}</div>
                        <div>
                          <Typography.Text strong>{item.citationLabel}</Typography.Text>
                          <p>{item.content}</p>
                        </div>
                      </article>
                    ))}
                  </div>
                  <div className="answer-panel">
                    <div className="answer-title">
                      <RobotOutlined />
                      <Typography.Title level={4}>问答草稿</Typography.Title>
                    </div>
                    {answerDraft ? (
                      <>
                        <Alert
                          type={answerDraft.evidenceSufficient ? 'success' : 'warning'}
                          showIcon
                          message={answerDraft.evidenceSufficient ? 'AI 生成，仅供人工确认' : '证据不足'}
                        />
                        <Typography.Paragraph>{answerDraft.answer}</Typography.Paragraph>
                      </>
                    ) : (
                      <Empty description="提交问题后生成草稿" />
                    )}
                  </div>
                </div>
              </section>
            )}
          </ProCard>
        </main>

        <aside className="knowledge-inspector">
          {currentSpace ? (
            <>
              <div className="inspector-title">
                <DatabaseOutlined />
                <div>
                  <Typography.Title level={3}>{currentSpace.name}</Typography.Title>
                  <Typography.Text type="secondary">{scopeLabel(currentSpace.scopeType)}</Typography.Text>
                </div>
              </div>
              <div className="inspector-actions">
                <Tooltip title="向当前知识库上传资料">
                  <Button type="primary" icon={<CloudUploadOutlined />} disabled={!currentSpace} onClick={() => setIngestOpen(true)}>
                    上传资料
                  </Button>
                </Tooltip>
                <Button icon={<FileSearchOutlined />} onClick={() => setActiveTab('search')}>检索问答</Button>
              </div>
              <div className="metric-strip">
                <div>
                  <strong>{documentRows.length}</strong>
                  <span>资料</span>
                </div>
                <div>
                  <strong>{sensitiveCount}</strong>
                  <span>敏感</span>
                </div>
                <div>
                  <strong>{roleCount}</strong>
                  <span>角色</span>
                </div>
              </div>
              <section className="permission-section">
                <Typography.Title level={4}>权限模型</Typography.Title>
                <Space direction="vertical" size={8}>
                  <Tag icon={<SafetyCertificateOutlined />} color="success">租户隔离</Tag>
                  <Tag color="processing">空间角色：{currentSpace.visibleRoleCodes.map(roleLabel).join(' / ') || '仅负责人'}</Tag>
                  <Tag icon={<LockOutlined />} color="error">敏感文档：restricted + 文档 ACL</Tag>
                </Space>
              </section>
              <Timeline
                className="index-timeline"
                items={[
                  { color: 'green', children: '文件由平台文件服务统一管理' },
                  { color: 'green', children: 'Tika 解析文本并按段落切片' },
                  { color: 'blue', children: '向量库字段已预留，后续接 Qdrant' },
                  { color: 'gray', children: 'AI 草稿必须人工确认' },
                ]}
              />
            </>
          ) : (
            <Empty description="请选择或创建知识库" />
          )}
        </aside>
      </div>

      <Drawer
        title="新建知识库"
        open={spaceOpen}
        width={480}
        onClose={() => setSpaceOpen(false)}
        destroyOnHidden
      >
        <Form
          form={spaceForm}
          layout="vertical"
          initialValues={{ scopeType: 'enterprise', status: 'enabled', visibleRoleCodes: ['PLATFORM_ADMIN'] }}
          onFinish={(values) => createSpaceMutation.mutate(values)}
        >
          <Form.Item name="name" label="知识库名称" rules={[{ required: true }]}>
            <Input placeholder="例如：企业制度知识库" />
          </Form.Item>
          <Form.Item name="code" label="知识库编码" rules={[{ required: true }]}>
            <Input placeholder="例如：enterprise-policy" />
          </Form.Item>
          <Form.Item name="scopeType" label="空间类型" rules={[{ required: true }]}>
            <Select options={scopeOptions} />
          </Form.Item>
          <Form.Item name="categoryCode" label="默认分类">
            <Select allowClear options={categoryOptions} />
          </Form.Item>
          <Form.Item name="visibleRoleCodes" label="可见角色">
            <Select mode="multiple" options={roleOptions} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={createSpaceMutation.isPending} block>
            创建
          </Button>
        </Form>
      </Drawer>

      <Drawer
        title="上传资料入库"
        open={ingestOpen}
        width={520}
        onClose={() => setIngestOpen(false)}
        destroyOnHidden
      >
        <Form
          form={ingestForm}
          layout="vertical"
          initialValues={{
            sourceType: 'file',
            categoryCode: currentSpace?.categoryCode,
            accessScope: 'inherit',
            sensitiveLevel: 'normal',
            visibleRoleCodes: ['PLATFORM_ADMIN'],
          }}
          onFinish={(values) => {
            if (!currentSpace || !uploadedFileId) {
              message.warning('请先选择知识库并上传文件');
              return;
            }
            ingestMutation.mutate({
              spaceId: currentSpace.id,
              payload: {
                ...values,
                fileId: uploadedFileId,
                visibleUserIds: values.visibleUserIdsText
                  ? String(values.visibleUserIdsText).split(',').map((item: string) => Number(item.trim())).filter(Boolean)
                  : undefined,
              },
            });
          }}
        >
          <Upload.Dragger {...uploadProps} className="upload-zone">
            <p className="ant-upload-drag-icon"><CloudUploadOutlined /></p>
            <p className="ant-upload-text">上传 PDF、Word、Excel、PPT、Markdown 或文本资料</p>
          </Upload.Dragger>
          <Form.Item name="title" label="资料标题" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="categoryCode" label="资料分类">
            <Select allowClear options={categoryOptions} />
          </Form.Item>
          <Form.Item name="accessScope" label="访问范围" rules={[{ required: true }]}>
            <Select
              options={[
                { label: '继承知识库空间权限', value: 'inherit' },
                { label: '敏感资料，仅文档 ACL 可见', value: 'restricted' },
              ]}
            />
          </Form.Item>
          <Form.Item name="sensitiveLevel" label="敏感等级">
            <Select
              options={[
                { label: '普通', value: 'normal' },
                { label: '内部', value: 'internal' },
                { label: '保密', value: 'confidential' },
              ]}
            />
          </Form.Item>
          <Form.Item name="visibleUserIdsText" label="签署人/负责人用户 ID">
            <Input placeholder="多个用户 ID 用英文逗号分隔" />
          </Form.Item>
          <Form.Item name="visibleRoleCodes" label="授权角色">
            <Select mode="multiple" options={roleOptions} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={ingestMutation.isPending} block>
            入库并建立索引
          </Button>
        </Form>
      </Drawer>
    </div>
  );
}
