import { useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import { DeleteOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../../api/training';
import type { SubsidyMaterialTemplate } from '../../../../api/training';
import { downloadUrlFile } from '../../../../api/files';

const MATERIAL_TYPE_OPTS = [
  { value: 'application_form', label: '申请表' },
  { value: 'roster', label: '花名册' },
  { value: 'summary', label: '汇总表' },
  { value: 'other', label: '其他' },
];

const SCOPE_OPTS = [
  { value: 'tenant', label: '机构库' },
  { value: 'class', label: '本班覆盖' },
  { value: 'platform', label: '平台' },
];

type Props = {
  open: boolean;
  onClose: () => void;
  classId?: number | string | null;
  regionCode?: string | null;
  trainMode?: string | null;
  policyYear?: number | null;
};

export function SubsidyMaterialTemplateLibraryModal({
  open,
  onClose,
  classId,
  regionCode,
  trainMode,
  policyYear,
}: Props) {
  const qc = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [uploadOpen, setUploadOpen] = useState(false);
  const [form] = Form.useForm();

  const listQ = useQuery({
    queryKey: ['subsidy-material-templates', classId, keyword],
    queryFn: () =>
      trainApi.listSubsidyMaterialTemplates({
        classId: classId ?? undefined,
        keyword: keyword || undefined,
        status: 'active',
      }),
    enabled: open,
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['subsidy-material-templates'] });
  };

  const uploadMu = useMutation({
    mutationFn: (values: {
      file: File;
      title?: string;
      materialType?: string;
      scope?: string;
      remark?: string;
    }) =>
      trainApi.uploadSubsidyMaterialTemplate({
        file: values.file,
        title: values.title,
        materialType: values.materialType,
        scope: values.scope || 'tenant',
        classId: values.scope === 'class' ? classId ?? undefined : undefined,
        regionCode: regionCode || undefined,
        trainMode: trainMode || undefined,
        policyYear: policyYear ?? undefined,
        remark: values.remark,
      }),
    onSuccess: () => {
      message.success('模板已上传');
      setUploadOpen(false);
      form.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '上传失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number | string) => trainApi.deleteSubsidyMaterialTemplate(id),
    onSuccess: () => {
      message.success('已删除');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const reparseMu = useMutation({
    mutationFn: (id: number | string) => trainApi.reparseSubsidyMaterialTemplate(id),
    onSuccess: () => {
      message.success('已重新解析占位符');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '解析失败'),
  });

  const rows = useMemo(() => listQ.data ?? [], [listQ.data]);

  return (
    <>
      <Modal
        title="补贴材料模板库"
        open={open}
        onCancel={onClose}
        width={900}
        footer={<Button onClick={onClose}>关闭</Button>}
        destroyOnClose
      >
        <Space wrap style={{ marginBottom: 12 }}>
          <Input.Search
            allowClear
            placeholder="搜索模板标题"
            style={{ width: 220 }}
            onSearch={setKeyword}
          />
          <Button type="primary" icon={<UploadOutlined />} onClick={() => setUploadOpen(true)}>
            上传模板
          </Button>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            支持 docx / xlsx 自动填数。花名册请选材料类型「花名册」，表头含「姓名」或原型行写 {'${name}'} / {'${idCard}'}。
          </Typography.Text>
        </Space>
        <Table<SubsidyMaterialTemplate>
          rowKey={(r) => String(r.id)}
          size="small"
          loading={listQ.isLoading}
          dataSource={rows}
          pagination={{ pageSize: 8 }}
          columns={[
            {
              title: '标题',
              dataIndex: 'title',
              ellipsis: true,
            },
            {
              title: '范围',
              dataIndex: 'scope',
              width: 88,
              render: (v: string) => SCOPE_OPTS.find((o) => o.value === v)?.label || v,
            },
            {
              title: '类型',
              dataIndex: 'materialTypeLabel',
              width: 88,
            },
            {
              title: '格式',
              dataIndex: 'fileFormat',
              width: 64,
              render: (v: string) => <Tag>{v}</Tag>,
            },
            {
              title: '占位符',
              dataIndex: 'fieldKeys',
              ellipsis: true,
              render: (keys?: string[]) =>
                keys?.length ? (
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {keys.slice(0, 6).join(', ')}
                    {keys.length > 6 ? ` +${keys.length - 6}` : ''}
                  </Typography.Text>
                ) : (
                  <Typography.Text type="secondary">—</Typography.Text>
                ),
            },
            {
              title: '操作',
              width: 200,
              render: (_: unknown, row) => (
                <Space size={4} wrap>
                  {row.templateFilePath ? (
                    <Button
                      type="link"
                      size="small"
                      onClick={() =>
                        void downloadUrlFile(row.templateFilePath!, `${row.title}.${row.fileFormat}`).catch(
                          () => message.error('下载失败'),
                        )
                      }
                    >
                      下载
                    </Button>
                  ) : null}
                  {row.fileFormat === 'docx' || row.fileFormat === 'xlsx' || row.fileFormat === 'xls' ? (
                    <Button
                      type="link"
                      size="small"
                      icon={<ReloadOutlined />}
                      loading={reparseMu.isPending}
                      onClick={() => reparseMu.mutate(row.id)}
                    >
                      重解析
                    </Button>
                  ) : null}
                  <Popconfirm title="确认删除该模板？" onConfirm={() => deleteMu.mutate(row.id)}>
                    <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </Modal>

      <Modal
        title="上传填数模板"
        open={uploadOpen}
        onCancel={() => {
          setUploadOpen(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        confirmLoading={uploadMu.isPending}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ scope: 'tenant', materialType: 'application_form' }}
          onFinish={(values) => {
            const file = values.file?.[0]?.originFileObj as File | undefined;
            if (!file) {
              message.warning('请选择文件');
              return;
            }
            if (values.scope === 'class' && classId == null) {
              message.warning('当前无班级，无法上传本班覆盖模板');
              return;
            }
            uploadMu.mutate({
              file,
              title: values.title,
              materialType: values.materialType,
              scope: values.scope,
              remark: values.remark,
            });
          }}
        >
          <Form.Item name="file" label="模板文件" rules={[{ required: true, message: '请上传文件' }]} valuePropName="fileList" getValueFromEvent={(e) => e?.fileList}>
            <Upload beforeUpload={() => false} maxCount={1} accept=".docx,.xlsx,.xls">
              <Button icon={<UploadOutlined />}>选择 docx / xlsx</Button>
            </Upload>
          </Form.Item>
          <Form.Item name="title" label="标题">
            <Input placeholder="默认取文件名" />
          </Form.Item>
          <Form.Item name="materialType" label="材料类型" rules={[{ required: true }]}>
            <Select options={MATERIAL_TYPE_OPTS} />
          </Form.Item>
          <Form.Item name="scope" label="入库范围" rules={[{ required: true }]}>
            <Select options={SCOPE_OPTS} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
