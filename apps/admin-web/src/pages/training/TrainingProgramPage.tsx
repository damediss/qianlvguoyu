import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, Upload, message } from 'antd';
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, InboxOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import type { TrainSolutionList, TrainSolutionDetail, TrainSolutionBadge, TrainSolutionSaveReq, DemandPublicData, CompanyOption } from '../../types/training';

const { Dragger } = Upload;
const badgeFeeOptions = ['劳务费', '住宿费', '耗材', '场地成本', '设备成本', '其他'];

export function TrainingProgramPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainSolutionSaveReq & { badgeTable?: TrainSolutionBadge[] }>();
  const [query, setQuery] = useState({ page: 1, pageSize: 10 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<TrainSolutionDetail | null>(null);

  // Public data (companies + train info list)
  const { data: publicData } = useQuery({
    queryKey: ['demand-public-data', ['company', 'traininfo']],
    queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
    staleTime: 5 * 60 * 1000,
  });

  // Solution list
  const { data: listData, isLoading } = useQuery({
    queryKey: ['solution-list', query],
    queryFn: () => trainApi.pageSolutionList(query),
  });

  // Save mutation
  const saveMu = useMutation({
    mutationFn: (p: TrainSolutionSaveReq) => trainApi.saveSolution(p),
    onSuccess: () => { message.success('保存成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['solution-list'] }); },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSolutionById(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['solution-list'] }); },
    onError: () => message.error('删除失败'),
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); form.setFieldsValue({ badgeList: [{ title: '', badge: 0, expend: 0 }] }); setEditingId(null); setModalOpen(true); };

  const handleEdit = async (row: TrainSolutionList) => {
    try {
      const d = await trainApi.getSolutionDetail(row.id);
      form.setFieldsValue({
        id: d.id,
        title: d.title,
        companyId: d.company_id,
        companyName: d.company_name,
        traininfoId: d.train_info_id,
        filePath: d.file_path,
        badgeList: (d.badgeList ?? []).map(b => ({ ...b })),
      });
      setEditingId(d.id);
      setModalOpen(true);
    } catch { message.error('获取详情失败'); }
  };

  const handleView = async (row: TrainSolutionList) => {
    try {
      const d = await trainApi.getSolutionDetail(row.id);
      setDetail(d);
      setDetailOpen(true);
    } catch { message.error('获取详情失败'); }
  };

  const companyOptions = (publicData as DemandPublicData | undefined)?.company_list?.map((c: CompanyOption) => ({ value: c.id, label: c.title })) ?? [];

  const handleSubmit = async () => {
    const v = await form.validateFields();
    const selectedCompany = companyOptions.find(c => c.value === v.companyId);
    saveMu.mutate({
      id: editingId ?? undefined,
      title: v.title,
      companyId: v.companyId,
      companyName: (selectedCompany?.label as string) ?? '',
      traininfoId: v.traininfoId,
      filePath: v.filePath,
      badgeList: v.badgeList ?? [],
    });
  };

  // File upload handler
  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.pdf,.docx',
    showUploadList: true,
    maxCount: 1,
    beforeUpload: (file) => {
      const isValid = file.type === 'application/pdf'
        || file.name.endsWith('.docx')
        || file.name.endsWith('.doc')
        || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
      if (!isValid) {
        message.error('只能上传 PDF 或 DOCX 文件');
        return Upload.LIST_IGNORE;
      }
      return true;
    },
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      try {
        const result = await uploadFile(file as File, 'public');
        const downloadUrl = `/api/files/${result.id}/download`;
        form.setFieldsValue({ filePath: downloadUrl });
        message.success('上传成功');
        onSuccess?.(result, file as unknown as XMLHttpRequest);
      } catch (err) {
        message.error('上传失败');
        onError?.(err as Error);
      }
    },
    onRemove: () => {
      form.setFieldsValue({ filePath: undefined });
    },
  };

  const columns: ColumnsType<TrainSolutionList> = [
    { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
    { title: '标题', dataIndex: 'title', width: 100 },
    { title: '企业', dataIndex: 'companyName' },
    { title: '培训需求', dataIndex: 'trainTitle', width: 180 },
    { title: '预算', dataIndex: 'badge', width: 100, align: 'center', render: (v: number) => v?.toLocaleString() ?? '-' },
    { title: '实际支出', dataIndex: 'expend', width: 100, align: 'center', render: (v: number) => v?.toLocaleString() ?? '-' },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
    {
      title: '操作', key: 'act', width: 220, fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleView(r)}>查看方案</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(r)}>修改</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(r.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>培训方案</Typography.Title>
          <Typography.Text type="secondary">管理企业培训方案，支持编制方案内容、配置费用明细</Typography.Text>
        </div>
      </section>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>添加</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns}
          dataSource={listData?.items ?? []}
          scroll={{ x: 1000 }}
          pagination={{
            current: query.page, pageSize: query.pageSize, total: listData?.total ?? 0,
            showSizeChanger: false, showTotal: (t) => `共 ${t} 条`,
            position: ['bottomRight'],
            onChange: (p) => setQuery(prev => ({ ...prev, page: p })),
          }}
        />
      </Card>

      {/* Create/Edit Dialog */}
      <Modal title={editingId ? '修改方案' : '新增方案'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={saveMu.isPending} destroyOnClose width={800}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={12}>
              <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
                <Input placeholder="请输入标题" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="companyId" label="受训单位" rules={[{ required: true, message: '请选择受训企业' }]}>
                <Select showSearch filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
                  placeholder="受训单位" allowClear options={companyOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={24}>
            <Col span={12}>
              <Form.Item name="traininfoId" label="绑定培训需求" rules={[{ required: true, message: '请选择绑定的需求' }]}>
                <Select showSearch filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
                  placeholder="培训需求" allowClear
                  options={(publicData as DemandPublicData | undefined)?.traininfo_list?.map((t: { id: number; title: string }) => ({ value: t.id, label: t.title })) ?? []} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="filePath" label="方案文档路径">
                <Input placeholder="输入文件路径或URL" />
              </Form.Item>
            </Col>
          </Row>

          {/* File upload */}
          <Form.Item label="上传方案文档">
            <Dragger {...uploadProps}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
              <p className="ant-upload-hint" style={{ color: '#ccc', marginTop: 10 }}>提示：上传过程中，请不要做任何操作</p>
            </Dragger>
            <Typography.Text type="secondary">只能上传 .pdf、.docx 文件</Typography.Text>
          </Form.Item>

          {/* Badge list */}
          <Typography.Text strong>预算管理</Typography.Text>
          <Form.List name="badgeList">
            {(fields, { add, remove }) => (
              <Table dataSource={fields.map((f, i) => ({ ...f, key: f.key, index: i }))}
                rowKey="key" size="small" pagination={false}
                style={{ marginTop: 8 }} bordered
                locale={{ emptyText: (
                  <Button type="dashed" onClick={() => add({ title: '', badge: 0, expend: 0 })} style={{ margin: 8 }}>
                    + 添加费用项
                  </Button>
                )}}>
                <Table.Column title="费用名" width={240}
                  render={(_: unknown, record: { key: number; name: number; index: number }) => (
                    <Form.Item name={[record.name, 'title']} noStyle rules={[{ required: true, message: '请选择' }]}>
                      <Select placeholder="选择费用名" style={{ width: '100%' }}
                        options={badgeFeeOptions.map(v => ({ value: v, label: v }))} />
                    </Form.Item>
                  )} />
                <Table.Column title="预算(元)" width={180}
                  render={(_: unknown, record: { key: number; name: number; index: number }) => (
                    <Form.Item name={[record.name, 'badge']} noStyle>
                      <InputNumber style={{ width: '100%' }} min={0} precision={2} />
                    </Form.Item>
                  )} />
                <Table.Column title="实际支出(元)" width={180}
                  render={(_: unknown, record: { key: number; name: number; index: number }) => (
                    <Form.Item name={[record.name, 'expend']} noStyle>
                      <InputNumber style={{ width: '100%' }} min={0} precision={2} />
                    </Form.Item>
                  )} />
                <Table.Column title="操作" width={60} align="center"
                  render={(_: unknown, record: { key: number; name: number; index: number }) =>
                    fields.length > 1 ? (
                      <Button type="link" danger onClick={() => remove(record.name)}>删除</Button>
                    ) : null
                  } />
              </Table>
            )}
          </Form.List>
          <Button type="dashed" onClick={() => {
            const badges = form.getFieldValue('badgeList') ?? [];
            form.setFieldsValue({ badgeList: [...badges, { title: '', badge: 0, expend: 0 }] });
          }} style={{ marginTop: 8 }} block>+ 添加费用项</Button>
        </Form>
      </Modal>

      {/* Detail Dialog */}
      <Modal title="培训方案" open={detailOpen} onCancel={() => { setDetailOpen(false); setDetail(null); }} footer={null} width="60%">
        {detail && (
          <div>
            <p style={{ marginTop: 10 }}><strong>方案标题：</strong>{detail.title}</p>
            {detail.reject_reason && <p style={{ marginTop: 10, color: '#ff4d4f' }}><strong>企业拒绝理由：</strong>{detail.reject_reason}</p>}
            <p style={{ marginTop: 10 }}><strong>费用明细：</strong></p>
            <Table dataSource={detail.badgeList ?? []} rowKey="title" size="small" bordered pagination={false}
              columns={[
                { title: '费用名称', dataIndex: 'title' },
                { title: '预算', dataIndex: 'badge', width: 180, render: (v: number) => v?.toLocaleString() },
                { title: '实际支出', dataIndex: 'expend', width: 180, render: (v: number) => v?.toLocaleString() },
              ]} />
            {detail.file && (
              <p style={{ marginTop: 10 }}>
                <strong>方案文档：</strong>
                <a href={detail.file} target="_blank" rel="noopener noreferrer">下载</a>
                {detail.fileType && <Tag style={{ marginLeft: 8 }}>{detail.fileType}</Tag>}
              </p>
            )}
            {/* Office document preview */}
            {detail.file && (detail.fileType === 'pdf' || detail.fileType === 'docx' || detail.fileType === 'pptx') && (
              <div style={{ height: 300, overflow: 'auto', border: '1px solid #e0e0e0', marginTop: 10 }}>
                {detail.fileType === 'pdf' ? (
                  <iframe src={detail.file} style={{ width: '100%', height: '100%', border: 'none' }} title="PDF预览" />
                ) : (
                  <iframe src={`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(detail.file)}`}
                    style={{ width: '100%', height: '100%', border: 'none' }} title="文档预览" />
                )}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
