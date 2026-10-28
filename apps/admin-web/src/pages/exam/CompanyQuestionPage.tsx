import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
  DownloadOutlined,
  InboxOutlined,
} from '@ant-design/icons';
import {
  Badge,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
  Popconfirm,
  Upload,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo, useEffect } from 'react';
import {
  pageCompanyQuestions,
  createCompanyQuestion,
  updateCompanyQuestion,
  deleteCompanyQuestion,
  batchDeleteCompanyQuestions,
  importCompanyQuestions,
  exportCompanyQuestions,
} from '../../api/exam';
import { listOrgs } from '../../api/system';
import { useAuthStore } from '../../stores/authStore';
import type {
  ExamExamDetail,
  CompanyQuestionCreateReq,
  CompanyQuestionUpdateReq,
  CompanyQuestionQuery,
} from '../../types/exam';
import {
  EXAM_TYPE_MAP,
  DIFFICULTY_MAP,
  DIFFICULTY_COLOR_MAP,
} from '../../types/exam';

const { TextArea } = Input;

// ─── CompanyQuestionFormModal ─────────────────────────────
interface FormModalProps {
  open: boolean;
  editing: ExamExamDetail | null;
  onClose: () => void;
}

function CompanyQuestionFormModal({ open, editing, onClose }: FormModalProps) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data: orgList } = useQuery({
    queryKey: ['company-question-org-list', open],
    queryFn: () => listOrgs(),
    enabled: open,
  });
  const orgOptions = useMemo(
    () => (orgList ?? []).map((o) => ({ value: o.id, label: o.name })),
    [orgList],
  );

  const createMut = useMutation({
    mutationFn: createCompanyQuestion,
    onSuccess: () => {
      message.success('创建成功');
      queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CompanyQuestionUpdateReq }) =>
      updateCompanyQuestion(id, payload),
    onSuccess: () => {
      message.success('更新成功');
      queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const handleOk = useCallback(async () => {
    try {
      const values = await form.validateFields();
      if (editing) {
        updateMut.mutate({ id: editing.id, payload: values });
      } else {
        createMut.mutate(values as CompanyQuestionCreateReq);
      }
    } catch {
      // validation error
    }
  }, [form, editing, createMut, updateMut]);

  useEffect(() => {
    if (open) {
      if (editing) {
        form.setFieldsValue({
          type: editing.type,
          content: editing.content,
          answer: editing.answer,
          orgId: editing.orgId,
          orgName: editing.orgName,
          difficulty: editing.difficulty,
          score: editing.score,
          resolve: editing.resolve,
          keyword: editing.keyword,
        });
      } else {
        form.resetFields();
      }
    }
  }, [open, editing, form]);

  return (
    <Modal
      title={editing ? '编辑试题' : '新建试题'}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={createMut.isPending || updateMut.isPending}
      width={720}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" initialValues={{ type: 'single', difficulty: 2 }}>
        <Form.Item name="type" label="题型" rules={[{ required: true, message: '请选择题型' }]}>
          <Select
            options={Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k }))}
            placeholder="选择题型"
          />
        </Form.Item>
        <Form.Item name="content" label="题干">
          <TextArea rows={4} placeholder="请输入题干内容" />
        </Form.Item>
        <Form.Item name="answer" label="答案">
          <TextArea rows={3} placeholder="请输入答案" />
        </Form.Item>
        <Form.Item
          name="orgId"
          label="绑定公司"
          rules={[{ required: true, message: '请选择公司' }]}
        >
          <Select
            showSearch
            allowClear
            placeholder="请选择公司"
            optionFilterProp="label"
            options={orgOptions}
            onChange={(value) => {
              const org = orgList?.find((o) => o.id === value);
              form.setFieldsValue({ orgName: org?.name });
            }}
          />
        </Form.Item>
        <Form.Item name="orgName" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="difficulty" label="难度">
          <Select
            options={Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
              label: v,
              value: Number(k),
            }))}
            placeholder="选择难度"
          />
        </Form.Item>
        <Form.Item name="score" label="分值">
          <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="分值" />
        </Form.Item>
        <Form.Item name="resolve" label="解析">
          <TextArea rows={3} placeholder="请输入解析" maxLength={2000} />
        </Form.Item>
        <Form.Item name="keyword" label="关键词">
          <Input placeholder="关键词，多个用逗号分隔" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// ─── Main Page ─────────────────────────────────────────────
export function CompanyQuestionPage() {
  const queryClient = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);

  // ── table state ──
  const [query, setQuery] = useState<CompanyQuestionQuery>({ page: 1, pageSize: 20 });
  const [filterOrgId, setFilterOrgId] = useState<number | undefined>();
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterDifficulty, setFilterDifficulty] = useState<number | undefined>();
  const [filterKeyword, setFilterKeyword] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // ── modal state ──
  const [modalOpen, setModalOpen] = useState(false);
  const [editingDetail, setEditingDetail] = useState<ExamExamDetail | null>(null);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importFileList, setImportFileList] = useState<any[]>([]);
  const [importLoading, setImportLoading] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);

  // ── org list for filter dropdown ──
  const { data: orgList } = useQuery({
    queryKey: ['company-question-filter-orgs'],
    queryFn: () => listOrgs(),
  });
  const orgFilterOptions = useMemo(
    () => (orgList ?? []).map((o) => ({ value: o.id, label: o.name })),
    [orgList],
  );

  // ── effective query ──
  const effectiveQuery = useMemo(
    () => ({
      ...query,
      orgId: filterOrgId,
      type: filterType,
      difficulty: filterDifficulty,
      keyword: filterKeyword || undefined,
    }),
    [query, filterOrgId, filterType, filterDifficulty, filterKeyword],
  );

  const { data: examPage, isLoading: loadingDetails } = useQuery({
    queryKey: ['companyQuestions', effectiveQuery],
    queryFn: () => pageCompanyQuestions(effectiveQuery),
  });

  // ── delete mutation ──
  const deleteMut = useMutation({
    mutationFn: deleteCompanyQuestion,
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  // ── batch delete mutation ──
  const batchDeleteMut = useMutation({
    mutationFn: batchDeleteCompanyQuestions,
    onSuccess: (n) => {
      message.success(`已删除 ${n} 道试题`);
      setSelectedRowKeys([]);
      queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
    },
    onError: (e: Error) => message.error(e.message || '批量删除失败'),
  });

  const handleBatchDelete = () => {
    const ids = selectedRowKeys.map(Number);
    if (ids.length === 0) return;
    Modal.confirm({
      title: `确认删除 ${ids.length} 道试题?`,
      content: '删除后无法恢复',
      okText: '确认删除',
      okButtonProps: { danger: true },
      onOk: () => batchDeleteMut.mutate(ids),
    });
  };

  // ── import handler ──
  const handleImport = async () => {
    if (!filterOrgId) {
      message.warning('请先在筛选栏选择公司');
      return;
    }
    if (importFileList.length === 0) {
      message.warning('请选择要导入的文件');
      return;
    }
    const org = orgList?.find((o) => o.id === filterOrgId);
    const formData = new FormData();
    formData.append('file', importFileList[0].originFileObj as Blob);
    formData.append('orgId', String(filterOrgId));
    formData.append('orgName', org?.name ?? '');
    formData.append('source', '0');

    setImportLoading(true);
    try {
      const result = await importCompanyQuestions(formData);
      message.success(
        `成功导入 ${result.importedCount} 道试题`
          + (result.warningCount > 0 ? `，另有 ${result.warningCount} 条格式提醒` : ''),
      );
      setImportModalOpen(false);
      setImportFileList([]);
      queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
      if (result.warningCount > 0) {
        Modal.info({
          title: `格式提醒（${result.warningCount} 条）`,
          width: 640,
          content: (
            <div>
              {result.warnings.slice(0, 20).map((issue, index) => (
                <div key={`${issue.row}-${issue.column}-${index}`} style={{ marginTop: 6 }}>
                  第 {issue.row} 行第 {issue.column} 列（{issue.field}）：{issue.message}
                </div>
              ))}
              {result.truncated && <div style={{ marginTop: 8 }}>仅展示前 20 条。</div>}
            </div>
          ),
        });
      }
    } catch (e: any) {
      message.error(e?.message || '导入失败');
    } finally {
      setImportLoading(false);
    }
  };

  // ── export handler ──
  const handleExport = async () => {
    setExportLoading(true);
    try {
      const resp = await exportCompanyQuestions(filterOrgId);
      const blob = new Blob([resp.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const disposition = resp.headers['content-disposition'];
      let filename = '公司试题导出.xlsx';
      if (disposition) {
        const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;\n]+)/i);
        if (match) filename = decodeURIComponent(match[1]);
      }
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (e: any) {
      message.error(e?.message || '导出失败');
    } finally {
      setExportLoading(false);
    }
  };

  // ── Table columns ──
  const columns: ColumnsType<ExamExamDetail> = [
    {
      title: '序号',
      width: 60,
      render: (_, __, index) => ((query.page ?? 1) - 1) * (query.pageSize ?? 20) + index + 1,
    },
    {
      title: '题型',
      dataIndex: 'type',
      width: 90,
      render: (v: string) => EXAM_TYPE_MAP[v] || v,
    },
    {
      title: '题干与选项',
      dataIndex: 'content',
      ellipsis: true,
      render: (v: string) => (
        <div style={{ maxHeight: 80, overflow: 'hidden' }}>
          {v ? v.substring(0, 120) : '-'}
        </div>
      ),
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 60,
      render: (v: number) => (
        <Tag color={DIFFICULTY_COLOR_MAP[v] || 'default'}>
          {DIFFICULTY_MAP[v] || v}
        </Tag>
      ),
    },
    {
      title: '公司名称',
      dataIndex: 'orgName',
      width: 250,
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 130,
      render: (v: string) => (v ? String(v).substring(0, 16) : '-'),
    },
    {
      title: '操作',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          {hasPerm('exam:question:update') && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingDetail(record);
                setModalOpen(true);
              }}
            >
              编辑
            </Button>
          )}
          {hasPerm('exam:question:delete') && (
            <Popconfirm
              title="确认删除此试题？"
              onConfirm={() => deleteMut.mutate(record.id)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {/* Filter bar */}
      <Space wrap>
        <Select
          style={{ width: 200 }}
          placeholder="公司"
          allowClear
          showSearch
          optionFilterProp="label"
          value={filterOrgId}
          onChange={(v) => {
            setFilterOrgId(v);
            setQuery((q) => ({ ...q, page: 1 }));
          }}
          options={orgFilterOptions}
        />
        <Select
          style={{ width: 120 }}
          placeholder="题型"
          allowClear
          value={filterType}
          onChange={(v) => {
            setFilterType(v);
            setQuery((q) => ({ ...q, page: 1 }));
          }}
          options={Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k }))}
        />
        <Select
          style={{ width: 100 }}
          placeholder="难度"
          allowClear
          value={filterDifficulty}
          onChange={(v) => {
            setFilterDifficulty(v);
            setQuery((q) => ({ ...q, page: 1 }));
          }}
          options={Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
            label: v,
            value: Number(k),
          }))}
        />
        <Input.Search
          style={{ width: 200 }}
          placeholder="搜索题干"
          allowClear
          onSearch={(v) => {
            setFilterKeyword(v);
            setQuery((q) => ({ ...q, page: 1 }));
          }}
        />
        <Button
          icon={<ReloadOutlined />}
          onClick={() => queryClient.invalidateQueries({ queryKey: ['companyQuestions'] })}
        >
          刷新
        </Button>
        {hasPerm('exam:question:delete') && (
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleBatchDelete}
          >
            <Badge count={selectedRowKeys.length} offset={[8, -2]} size="small" showZero={false}>
              <span>批量删除</span>
            </Badge>
          </Button>
        )}
        {hasPerm('exam:question:create') && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingDetail(null);
              setModalOpen(true);
            }}
          >
            新建试题
          </Button>
        )}
        {hasPerm('exam:question:create') && (
          <Button
            icon={<UploadOutlined />}
            onClick={() => {
              if (!filterOrgId) {
                message.warning('请先在筛选栏选择公司');
                return;
              }
              setImportFileList([]);
              setImportModalOpen(true);
            }}
          >
            导入试题
          </Button>
        )}
        {hasPerm('exam:question:view') && (
          <Button icon={<DownloadOutlined />} loading={exportLoading} onClick={handleExport}>
            导出试题
          </Button>
        )}
      </Space>

      {/* Table */}
      <Table<ExamExamDetail>
        rowKey="id"
        columns={columns}
        dataSource={examPage?.items ?? []}
        loading={loadingDetails}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as React.Key[]),
        }}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total: examPage?.total ?? 0,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, pageSize) => setQuery((q) => ({ ...q, page, pageSize })),
        }}
        size="small"
        scroll={{ x: 1000 }}
      />

      {/* ── Form Modal ── */}
      <CompanyQuestionFormModal
        open={modalOpen}
        editing={editingDetail}
        onClose={() => {
          setModalOpen(false);
          setEditingDetail(null);
        }}
      />

      {/* ── Import Modal ── */}
      <Modal
        title={`导入公司试题${filterOrgId ? ` — ${orgList?.find((o) => o.id === filterOrgId)?.name ?? ''}` : ''}`}
        open={importModalOpen}
        onOk={handleImport}
        onCancel={() => {
          setImportModalOpen(false);
          setImportFileList([]);
        }}
        confirmLoading={importLoading}
        okText="开始导入"
        destroyOnHidden
      >
        <Upload.Dragger
          accept=".xls,.xlsx"
          maxCount={1}
          fileList={importFileList}
          beforeUpload={() => false}
          onChange={({ fileList }) => setImportFileList(fileList)}
        >
          <p style={{ fontSize: 40, color: '#1677ff', marginBottom: 8 }}>
            <InboxOutlined />
          </p>
          <p style={{ fontSize: 14 }}>点击或将文件拖拽到这里上传</p>
          <p style={{ color: '#999', fontSize: 12 }}>支持 .xls / .xlsx 格式</p>
        </Upload.Dragger>
      </Modal>
    </div>
  );
}
