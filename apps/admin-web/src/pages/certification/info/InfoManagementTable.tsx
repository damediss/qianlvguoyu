import { DeleteOutlined, EditOutlined, ExportOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, Typography, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { FormInstance, UploadFile } from 'antd';
import dayjs from 'dayjs';
import { useMemo, useState, type ReactNode } from 'react';
import { uploadPlatformFile } from '../../../api/files';
import { treeWorkTypes } from '../../../api/exam';
import { FilePreviewModal, type FilePreviewTarget } from '../../../components/FilePreviewModal';
import { LevelScopeRangeSelect } from '../../../components/LevelScopeRangeSelect';
import { PermissionGate } from '../../../directives/permission';
import type { PageResponse } from '../../../types/api';
import type { CertificationInfoQuery, InfoReviewPayload } from '../../../types/certificationInfo';
import { flattenWorkTypeOptions } from '../../../utils/workTypeOptions';

type FieldType = 'text' | 'password' | 'textarea' | 'date' | 'number' | 'select' | 'select-multiple' | 'work-type-multiple' | 'levelRange' | 'imageUpload';

export interface InfoField<T> {
  name: keyof T & string;
  label: string;
  type?: FieldType;
  required?: boolean;
  createRequired?: boolean;
  submitRequired?: boolean;
  options?: { label: string; value: string }[];
  placeholder?: string;
  fileNameField?: keyof T & string;
  hidden?: boolean;
  hideInView?: boolean;
  hideInEdit?: boolean;
}

interface InfoManagementTableProps<T extends { id: string; auditStatus: string }, P extends object> {
  title: string;
  queryKey: string;
  permissionPrefix: string;
  rowKey: keyof T & string;
  statusField: keyof T & string;
  columns: ColumnsType<T>;
  auditColumnIndex?: number;
  fields: InfoField<P>[];
  statusOptions: { label: string; value: string }[];
  editableAuditStatuses?: string[];
  showStatusFilter?: boolean;
  pageApi: (params: CertificationInfoQuery) => Promise<PageResponse<T>>;
  createApi: (payload: P) => Promise<T>;
  updateApi: (id: string, payload: P) => Promise<T>;
  submitApi: (id: string) => Promise<T>;
  reviewApi: (id: string, payload: InfoReviewPayload) => Promise<T>;
  disableApi: (id: string, payload: { reason?: string }) => Promise<void>;
  enableApi: (id: string) => Promise<void>;
  deleteApi: (id: string) => Promise<void>;
  createAccountApi?: (id: string, payload: { username: string; password: string }) => Promise<T>;
  toolbarExtra?: ReactNode;
}

const auditOptions = [
  { label: '待审核', value: 'draft' },
  { label: '待审核', value: 'pending' },
  { label: '已入库', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已停用', value: 'disabled' },
];

const auditColor: Record<string, string> = {
  draft: 'default',
  pending: 'processing',
  approved: 'success',
  rejected: 'error',
  disabled: 'warning',
};

const deletableAuditStatuses = ['draft', 'rejected', 'disabled'];

type DeletableRecord = {
  auditStatus: string;
  workStatus?: string;
  status?: string;
  equipmentStatus?: string;
  userId?: string;
};

function isRecordDeletable(record: DeletableRecord) {
  if (deletableAuditStatuses.includes(record.auditStatus)) {
    return true;
  }
  return record.workStatus === 'disabled'
    || record.status === 'disabled'
    || record.equipmentStatus === 'disabled';
}

export function InfoManagementTable<T extends { id: string; auditStatus: string } & DeletableRecord, P extends object>({
  title,
  queryKey,
  permissionPrefix,
  rowKey,
  statusField,
  columns,
  auditColumnIndex,
  fields,
  statusOptions,
  editableAuditStatuses = ['draft', 'rejected'],
  showStatusFilter = true,
  pageApi,
  createApi,
  updateApi,
  submitApi,
  reviewApi,
  disableApi,
  enableApi,
  deleteApi,
  createAccountApi,
  toolbarExtra,
}: InfoManagementTableProps<T, P>) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const [reviewForm] = Form.useForm();
  const [query, setQuery] = useState<CertificationInfoQuery>({ page: 1, pageSize: 10 });
  const [modalMode, setModalMode] = useState<'create' | 'edit' | 'view' | null>(null);
  const [current, setCurrent] = useState<T | null>(null);
  const [reviewing, setReviewing] = useState<T | null>(null);
  const [accountRecord, setAccountRecord] = useState<T | null>(null);
  const [accountForm] = Form.useForm<{ username: string; password: string }>();

  const dataQuery = useQuery({
    queryKey: [queryKey, query],
    queryFn: () => pageApi(query),
  });

  const hasWorkTypeField = fields.some((field) => field.type === 'work-type-multiple');
  const workTypeQuery = useQuery({
    queryKey: ['exam-work-types-tree'],
    queryFn: treeWorkTypes,
    enabled: hasWorkTypeField,
    staleTime: 5 * 60 * 1000,
  });
  const workTypeOptions = useMemo(
    () => flattenWorkTypeOptions(workTypeQuery.data ?? []),
    [workTypeQuery.data],
  );

  const invalidate = () => queryClient.invalidateQueries({ queryKey: [queryKey] });

  const saveMutation = useMutation({
    mutationFn: async (values: P) => {
      const normalized = normalizePayload(values, fields) as P;
      return current ? updateApi(current.id, normalized) : createApi(normalized);
    },
    onSuccess: () => {
      message.success('保存成功');
      setModalMode(null);
      setCurrent(null);
      invalidate();
    },
  });

  const actionMutation = useMutation({
    mutationFn: async (task: { action: 'submit' | 'disable' | 'enable' | 'delete'; record: T; reason?: string }) => {
      if (task.action === 'submit') return submitApi(task.record.id);
      if (task.action === 'disable') return disableApi(task.record.id, { reason: task.reason });
      if (task.action === 'enable') return enableApi(task.record.id);
      return deleteApi(task.record.id);
    },
    onSuccess: () => {
      message.success('操作成功');
      invalidate();
    },
  });

  const reviewMutation = useMutation({
    mutationFn: (payload: InfoReviewPayload) => reviewApi(reviewing!.id, payload),
    onSuccess: () => {
      message.success('审核完成');
      setReviewing(null);
      reviewForm.resetFields();
      invalidate();
    },
  });

  const accountMutation = useMutation({
    mutationFn: (payload: { username: string; password: string }) =>
      createAccountApi!(accountRecord!.id, payload),
    onSuccess: () => {
      message.success('平板登录账号已创建');
      setAccountRecord(null);
      accountForm.resetFields();
      invalidate();
    },
  });

  const finalColumns = useMemo<ColumnsType<T>>(() => {
    const auditColumn = {
      title: '审核状态',
      dataIndex: 'auditStatus',
      width: 120,
      render: (value: string) => <Tag color={auditColor[value] || 'default'}>{labelOf(auditOptions, value)}</Tag>,
    } satisfies ColumnsType<T>[number];

    const actionColumn = {
      title: '操作',
      width: 260,
      fixed: 'right',
      render: (_: unknown, record: T) => (
        <Space size={4} wrap>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openModal('view', record)}>查看</Button>
          {createAccountApi && !record.userId ? (
            <PermissionGate code={`${permissionPrefix}.update`}>
              <Button size="small" onClick={() => setAccountRecord(record)}>创建账号</Button>
            </PermissionGate>
          ) : null}
          <PermissionGate code={`${permissionPrefix}.update`}>
            <Button
              size="small"
              icon={<EditOutlined />}
              disabled={!editableAuditStatuses.includes(record.auditStatus)}
              onClick={() => openModal('edit', record)}
            >
              编辑
            </Button>
          </PermissionGate>
          <PermissionGate code={`${permissionPrefix}.submit`}>
            <Button
              size="small"
              disabled={!['draft', 'rejected'].includes(record.auditStatus)}
              onClick={() => actionMutation.mutate({ action: 'submit', record })}
            >
              提交
            </Button>
          </PermissionGate>
          <PermissionGate code={`${permissionPrefix}.review`}>
            <Button size="small" disabled={record.auditStatus !== 'pending'} onClick={() => setReviewing(record)}>审核</Button>
          </PermissionGate>
          <PermissionGate code={`${permissionPrefix}.update`}>
            {record.auditStatus === 'disabled' ? (
              <Popconfirm title="确认启用该记录？" onConfirm={() => actionMutation.mutate({ action: 'enable', record })}>
                <Button size="small" type="primary">启用</Button>
              </Popconfirm>
            ) : (
              <Popconfirm title="确认停用该记录？" onConfirm={() => actionMutation.mutate({ action: 'disable', record, reason: '主管停用' })}>
                <Button size="small" disabled={record.auditStatus !== 'approved'}>停用</Button>
              </Popconfirm>
            )}
          </PermissionGate>
          <PermissionGate code={`${permissionPrefix}.delete`}>
            <Popconfirm title="确认删除该记录？" onConfirm={() => actionMutation.mutate({ action: 'delete', record })}>
              <Button size="small" danger icon={<DeleteOutlined />} disabled={!isRecordDeletable(record)} />
            </Popconfirm>
          </PermissionGate>
        </Space>
      ),
    } satisfies ColumnsType<T>[number];

    const base = [...columns];
    const index =
      typeof auditColumnIndex === 'number' && Number.isFinite(auditColumnIndex)
        ? Math.max(0, Math.min(auditColumnIndex, base.length))
        : base.length;
    base.splice(index, 0, auditColumn);
    base.push(actionColumn);
    return base;
  }, [actionMutation, auditColumnIndex, columns, createAccountApi, editableAuditStatuses, permissionPrefix]);

  const openModal = (mode: 'create' | 'edit' | 'view', record?: T) => {
    setModalMode(mode);
    setCurrent(record || null);
    form.resetFields();
    if (record) {
      const values: Record<string, unknown> = { ...record };
      for (const field of fields) {
        if (field.type === 'date' && typeof values[field.name] === 'string') {
          values[field.name] = dayjs(values[field.name] as string);
        }
        if (field.name === 'phone' && !values.phone && values.maskedPhone) {
          values.phone = values.maskedPhone;
        }
      }
      form.setFieldsValue(values);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>{title}</Typography.Title>
          <Typography.Text type="secondary">基础档案建档、提交审核、主管审核和状态维护</Typography.Text>
        </div>
        <Space>
          {toolbarExtra}
          <PermissionGate code={`${permissionPrefix}.export`}>
            <Button icon={<ExportOutlined />} onClick={() => message.info('导出任务已提交')}>导出</Button>
          </PermissionGate>
          <PermissionGate code={`${permissionPrefix}.create`}>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal('create')}>新增</Button>
          </PermissionGate>
        </Space>
      </Space>

      <Space wrap>
        <Input.Search
          allowClear
          placeholder="关键词搜索"
          style={{ width: 260 }}
          onSearch={(keyword) => setQuery((old) => ({ ...old, keyword, page: 1 }))}
        />
        <Select
          allowClear
          placeholder="审核状态"
          options={auditOptions}
          style={{ width: 160 }}
          onChange={(auditStatus) => setQuery((old) => ({ ...old, auditStatus, page: 1 }))}
        />
        {showStatusFilter && statusOptions.length > 0 ? (
          <Select
            allowClear
            placeholder="运行状态"
            options={statusOptions}
            style={{ width: 180 }}
            onChange={(status) => setQuery((old) => ({ ...old, status, page: 1 }))}
          />
        ) : null}
      </Space>

      <Table
        rowKey={rowKey}
        columns={finalColumns}
        dataSource={dataQuery.data?.items || []}
        loading={dataQuery.isLoading}
        scroll={{ x: 1180 }}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total: dataQuery.data?.total || 0,
          showSizeChanger: true,
          onChange: (page, pageSize) => setQuery((old) => ({ ...old, page, pageSize })),
        }}
      />

      <Modal
        title={modalTitle(title, modalMode)}
        open={!!modalMode}
        onCancel={() => setModalMode(null)}
        onOk={() => form.submit()}
        okButtonProps={{ disabled: modalMode === 'view', loading: saveMutation.isPending }}
        width={760}
      >
        <Form form={form} layout="vertical" disabled={modalMode === 'view'} onFinish={(values) => saveMutation.mutate(values as P)}>
          {fields.filter((field) =>
            !(modalMode === 'view' && field.hideInView)
            && !(modalMode === 'edit' && field.hideInEdit)
          ).map((field) => (
            <Form.Item
              key={field.name}
              name={field.name as never}
              label={field.label}
              hidden={field.hidden}
              rules={[{
                required: field.required || (modalMode === 'create' && field.createRequired),
                message: `请填写${field.label}`,
              }]}
            >
              {renderField(
                field,
                form,
                modalMode === 'view',
                workTypeOptions,
                workTypeQuery.isLoading,
                workTypeQuery.isError,
              )}
            </Form.Item>
          ))}
        </Form>
      </Modal>

      <Modal
        title={`主管审核 - ${reviewing ? String(reviewing[statusField]) : ''}`}
        open={!!reviewing}
        onCancel={() => setReviewing(null)}
        footer={[
          <Button key="reject" danger loading={reviewMutation.isPending} onClick={() => reviewForm.validateFields().then((values) => reviewMutation.mutate({ reviewAction: 'reject', comment: values.comment }))}>驳回</Button>,
          <Button key="pass" type="primary" loading={reviewMutation.isPending} onClick={() => reviewMutation.mutate({ reviewAction: 'pass' })}>通过</Button>,
        ]}
      >
        <Form form={reviewForm} layout="vertical">
          <Form.Item name="comment" label="驳回原因" rules={[{ min: 6, message: '驳回原因不少于 6 个字符' }]}>
            <Input.TextArea rows={4} placeholder="驳回时必填" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`创建平板账号 - ${accountRecord ? String(accountRecord[statusField]) : ''}`}
        open={Boolean(accountRecord)}
        confirmLoading={accountMutation.isPending}
        onCancel={() => {
          setAccountRecord(null);
          accountForm.resetFields();
        }}
        onOk={() => accountForm.validateFields().then((values) => accountMutation.mutate(values))}
      >
        <Form form={accountForm} layout="vertical">
          <Form.Item name="username" label="平板登录账号" rules={[{ required: true, message: '请输入平板登录账号' }]}>
            <Input autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="password"
            label="平板登录密码"
            rules={[
              { required: true, message: '请输入平板登录密码' },
              { min: 8, message: '密码至少 8 位' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

function renderField<T>(
  field: InfoField<T>,
  form: FormInstance,
  readOnly = false,
  workTypeOptions: { label: string; value: string }[] = [],
  workTypeLoading = false,
  workTypeError = false,
) {
  if (field.type === 'password') return readOnly ? <SecretText /> : <Input.Password autoComplete="new-password" />;
  if (field.type === 'textarea') return <Input.TextArea rows={3} />;
  if (field.type === 'date') return <DatePicker style={{ width: '100%' }} />;
  if (field.type === 'number') return <InputNumber min={1} style={{ width: '100%' }} />;
  if (field.type === 'select') return <Select options={field.options || []} />;
  if (field.type === 'select-multiple') return (
    <Select
      mode="tags"
      allowClear
      showSearch
      maxTagCount="responsive"
      placeholder={field.placeholder}
      tokenSeparators={[',', '，', '、']}
    />
  );
  if (field.type === 'work-type-multiple') return (
    <Select
      mode="multiple"
      allowClear
      showSearch
      optionFilterProp="label"
      maxTagCount="responsive"
      placeholder={field.placeholder}
      options={workTypeOptions}
      loading={workTypeLoading}
      notFoundContent={workTypeError ? '工种数据加载失败' : undefined}
    />
  );
  if (field.type === 'levelRange') return <LevelScopeRangeSelect />;
  if (field.type === 'imageUpload') return <ImageUploadField field={field} form={form} readOnly={readOnly} />;
  return <Input />;
}

function SecretText({ value }: { value?: string }) {
  if (!value) {
    return <Typography.Text type="secondary">未记录</Typography.Text>;
  }
  return <Typography.Text copyable code>{value}</Typography.Text>;
}

function ImageUploadField<T>({ field, form, value, onChange, readOnly }: {
  field: InfoField<T>;
  form: FormInstance;
  value?: string | number;
  onChange?: (value?: string) => void;
  readOnly?: boolean;
}) {
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);
  const fileName = field.fileNameField ? form.getFieldValue(field.fileNameField) : undefined;
  const fileList: UploadFile[] = value
    ? [{ uid: String(value), name: fileName || '设备照片', status: 'done' }]
    : [];

  const openPreview = () => {
    const fileId = String(value || '');
    if (!fileId) {
      return;
    }
    setPreviewTarget({
      fileId,
      fileName: fileName || '设备照片',
      fileType: 'image/*',
    });
  };

  if (readOnly) {
    return (
      <>
        {value ? (
          <Button type="link" size="small" style={{ paddingInline: 0 }} disabled={false} onClick={openPreview}>
            查看照片
          </Button>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        )}
        <FilePreviewModal
          open={Boolean(previewTarget)}
          target={previewTarget}
          onClose={() => setPreviewTarget(null)}
        />
      </>
    );
  }

  return (
    <>
      <Upload
      accept="image/*"
      disabled={false}
      maxCount={1}
      fileList={fileList}
      beforeUpload={async (file) => {
        if (readOnly) {
          return Upload.LIST_IGNORE;
        }
        if (!file.type.startsWith('image/')) {
          message.warning('请上传图片文件');
          return Upload.LIST_IGNORE;
        }
        const uploaded = await uploadPlatformFile(file, 'private');
        onChange?.(String(uploaded.id));
        if (field.fileNameField) {
          form.setFieldValue(field.fileNameField, uploaded.originalName);
        }
        message.success('设备照片已上传');
        return Upload.LIST_IGNORE;
      }}
      onPreview={(file) => {
        const fileId = String(file.uid || value || '');
        if (!fileId) {
          return;
        }
        setPreviewTarget({
          fileId,
          fileName: file.name || fileName || '设备照片',
          fileType: 'image/*',
        });
      }}
      onRemove={() => {
        if (readOnly) {
          return false;
        }
        onChange?.(undefined);
        if (field.fileNameField) {
          form.setFieldValue(field.fileNameField, undefined);
        }
      }}
      showUploadList={{
        showPreviewIcon: true,
        showRemoveIcon: !readOnly,
      }}
      listType="picture"
    >
      <Button>上传照片</Button>
      </Upload>
      <FilePreviewModal
        open={Boolean(previewTarget)}
        target={previewTarget}
        onClose={() => setPreviewTarget(null)}
      />
    </>
  );
}

function normalizePayload<T>(values: T, fields: InfoField<T>[]) {
  const next = { ...(values as Record<string, unknown>) };
  for (const field of fields) {
    if (field.type === 'date' && next[field.name] && dayjs.isDayjs(next[field.name])) {
      next[field.name] = (next[field.name] as dayjs.Dayjs).format('YYYY-MM-DD');
    }
  }
  return next;
}

function labelOf(options: { label: string; value: string }[], value: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

function modalTitle(title: string, mode: 'create' | 'edit' | 'view' | null) {
  if (mode === 'create') return `新增${title}`;
  if (mode === 'edit') return `编辑${title}`;
  if (mode === 'view') return `查看${title}`;
  return title;
}
