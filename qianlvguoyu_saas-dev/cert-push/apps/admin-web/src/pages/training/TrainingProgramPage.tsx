/**
 * 培训方案 — 对齐 yun_shang trainingProgram.vue
 * 单表 + 编辑弹窗 + 查看弹窗；接口 /api/training/solutions/*
 */
import { useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Upload,
  message,
} from 'antd';
import { PlusOutlined, DeleteOutlined, InboxOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import type {
  TrainSolutionList,
  TrainSolutionDetail,
  TrainSolutionBadge,
  TrainSolutionSaveReq,
  DemandPublicData,
} from '../../types/training';
import './TrainingProgramPage.css';

const { Dragger } = Upload;
const BADGE_FEE_OPTIONS = ['劳务费', '住宿费', '耗材', '场地成本', '设备成本', '其他'];

const TABLE_HEADER_STYLE = { background: '#f8f9fa' };

function resolveFileUrl(path?: string): string {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  if (path.startsWith('/')) return path;
  return `/${path}`;
}

function fileTypeFromPath(path?: string): string {
  if (!path || !path.includes('.')) return '';
  return path.substring(path.lastIndexOf('.') + 1).toLowerCase();
}

export function TrainingProgramPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainSolutionSaveReq>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [editOpen, setEditOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [viewOpen, setViewOpen] = useState(false);
  const [trainProgram, setTrainProgram] = useState<TrainSolutionDetail | null>(null);
  const [filePathTip, setFilePathTip] = useState('');

  const { data: publicData } = useQuery({
    queryKey: ['demand-public-data', 'program'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
    staleTime: 5 * 60 * 1000,
  });

  const { data: listData, isLoading } = useQuery({
    queryKey: ['solution-list', currentPage, pageSize],
    queryFn: () => trainApi.pageSolutionList({ page: currentPage, pageSize }),
  });

  const saveMu = useMutation({
    mutationFn: (p: TrainSolutionSaveReq) => trainApi.saveSolution(p),
    onSuccess: (res) => {
      if (res?.info) {
        message.success('提交成功');
        closeEdit();
        queryClient.invalidateQueries({ queryKey: ['solution-list'] });
      }
    },
    onError: () => message.error('提交失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSolutionById(id),
    onSuccess: () => {
      message.success('删除成功！');
      queryClient.invalidateQueries({ queryKey: ['solution-list'] });
    },
    onError: () => message.error('删除失败'),
  });

  const companyOptions =
    (publicData as DemandPublicData | undefined)?.company_list?.map((c) => ({
      value: c.id,
      label: c.title,
    })) ?? [];

  const traininfoOptions =
    (publicData as DemandPublicData | undefined)?.traininfo_list?.map((t) => ({
      value: t.id,
      label: t.title,
    })) ?? [];

  const closeEdit = () => {
    setEditOpen(false);
    setEditingId(null);
    setFilePathTip('');
    form.resetFields();
  };

  const openAdd = () => {
    form.resetFields();
    form.setFieldsValue({
      badgeList: [{ title: '', badge: undefined, expend: undefined }],
    } as TrainSolutionSaveReq);
    setEditingId(null);
    setFilePathTip('');
    setEditOpen(true);
  };

  const mapDetailToForm = (d: TrainSolutionDetail, id: number) => {
    const badges = (d.badgeList ?? []).map((b) => ({
      title: b.title,
      badge: b.badge != null ? Number(b.badge) : undefined,
      expend: b.expend != null ? Number(b.expend) : undefined,
    }));
    form.setFieldsValue({
      id,
      title: d.title,
      companyId: d.company_id,
      traininfoId: d.train_info_id,
      filePath: d.file_path,
      badgeList: badges.length > 0 ? badges : [{ title: '', badge: undefined, expend: undefined }],
    });
    setFilePathTip(d.file_path ?? '');
    setEditingId(id);
    setEditOpen(true);
  };

  const handleView = async (row: TrainSolutionList) => {
    try {
      const d = await trainApi.getSolutionDetail(row.id);
      const file = resolveFileUrl(d.file || d.file_path);
      setTrainProgram({
        ...d,
        file,
        fileType: d.fileType || fileTypeFromPath(file),
        badgeList: (d.badgeList ?? []).map((b) => ({
          ...b,
          badge: Number(b.badge),
          expend: Number(b.expend),
        })),
      });
      setViewOpen(true);
    } catch {
      message.error('获取信息失败');
    }
  };

  const handleEdit = async (row: TrainSolutionList) => {
    try {
      const d = await trainApi.getSolutionDetail(row.id);
      mapDetailToForm(d, row.id);
    } catch {
      message.error('获取信息失败');
    }
  };

  const handleDelete = (row: TrainSolutionList) => {
    Modal.confirm({
      title: '提示',
      content: '此操作将永久删除该记录，是否继续？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => deleteMu.mutateAsync(row.id),
    });
  };

  const confirmSave = async () => {
    try {
      const v = await form.validateFields();
      const company = companyOptions.find((c) => c.value === v.companyId);
      const payload: TrainSolutionSaveReq = {
        id: editingId ?? undefined,
        title: v.title,
        companyId: v.companyId,
        companyName: (company?.label as string) ?? '',
        traininfoId: v.traininfoId,
        filePath: v.filePath || filePathTip,
        badgeList: (v.badgeList ?? []) as TrainSolutionBadge[],
      };
      saveMu.mutate(payload);
    } catch {
      message.error('表单错误');
    }
  };

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.pdf,.docx',
    maxCount: 1,
    showUploadList: false,
    className: 'upload-demos',
    beforeUpload: (file) => {
      const ok =
        file.name.endsWith('.pdf') ||
        file.name.endsWith('.docx') ||
        file.type === 'application/pdf' ||
        file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
      if (!ok) {
        message.error('只能上传 .pdf、.docx 文件');
        return Upload.LIST_IGNORE;
      }
      return true;
    },
    customRequest: async ({ file, onSuccess, onError }) => {
      try {
        const result = await uploadFile(file as File, 'public');
        const url = `/api/files/${result.id}/download`;
        form.setFieldsValue({ filePath: url });
        setFilePathTip(url);
        message.success('上传成功');
        onSuccess?.(result);
      } catch (e) {
        message.error('上传失败');
        onError?.(e as Error);
      }
    },
  };

  const columns: ColumnsType<TrainSolutionList> = [
    { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
    { title: '标题', dataIndex: 'title', align: 'center', width: 100 },
    { title: '企业', dataIndex: 'companyName', align: 'center' },
    { title: '培训需求', dataIndex: 'trainTitle', align: 'center', width: 180 },
    { title: '预算', dataIndex: 'badge', align: 'center', width: 100 },
    { title: '实际支出', dataIndex: 'expend', align: 'center', width: 100 },
    { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
    {
      title: '操作',
      align: 'center',
      width: 220,
      render: (_, row) => (
        <Space size="small">
          <Button type="link" className="link-view" onClick={() => handleView(row)}>
            查看方案
          </Button>
          <Button type="link" className="link-edit" onClick={() => handleEdit(row)}>
            修改
          </Button>
          <Button type="link" className="link-delete" onClick={() => handleDelete(row)}>
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const viewColumns: ColumnsType<TrainSolutionBadge> = [
    { title: '费用名称', dataIndex: 'title' },
    { title: '预算', dataIndex: 'badge', width: 180 },
  ];

  return (
    <div className="training-program-page expense-table-container">
      <Button type="primary" onClick={openAdd} style={{ marginBottom: 8 }}>
        + 添加
      </Button>

      <Table
        rowKey="id"
        loading={isLoading}
        columns={columns}
        dataSource={listData?.items ?? []}
        pagination={false}
        scroll={{ y: 600 }}
        style={{ width: '100%' }}
      />

      <div className="pagination-bar">
        <Pagination
          current={currentPage}
          pageSize={pageSize}
          total={listData?.total ?? 0}
          showSizeChanger={false}
          showTotal={(t) => `共 ${t} 条`}
          onChange={(p) => setCurrentPage(p)}
        />
      </div>

      {/* 编辑弹窗 — 对齐 el-dialog width 50% */}
      <Modal
        title={editingId ? '修改' : '新增'}
        open={editOpen}
        onCancel={closeEdit}
        onOk={confirmSave}
        confirmLoading={saveMu.isPending}
        okText="确认"
        cancelText="取消"
        width="50%"
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 18 }}>
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入标题" />
          </Form.Item>
          <Form.Item name="companyId" label="受训单位" rules={[{ required: true, message: '请选择受训企业' }]}>
            <Select showSearch allowClear placeholder="受训单位" options={companyOptions}
              filterOption={(input, opt) => (opt?.label as string ?? '').includes(input)} />
          </Form.Item>
          <Form.Item name="traininfoId" label="绑定培训需求" rules={[{ required: true, message: '请选择绑定的需求' }]}>
            <Select showSearch allowClear placeholder="培训需求" options={traininfoOptions}
              filterOption={(input, opt) => (opt?.label as string ?? '').includes(input)} />
          </Form.Item>
          <Form.Item name="filePath" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="方案文档">
            <Dragger {...uploadProps}>
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">
                将文件拖到此处，或<em>点击上传</em>
              </p>
              <p className="ant-upload-hint" style={{ color: '#ccc', marginTop: 10 }}>
                提示:上传过程中,请不要做任何操作
              </p>
            </Dragger>
            <div style={{ marginTop: 8, fontSize: 12, color: 'rgba(0,0,0,0.45)' }}>
              只能上传 .pdf、.docx 文件
              {filePathTip ? <span className="upload-tip-path">{filePathTip}</span> : null}
            </div>
          </Form.Item>
          <Form.Item label="预算管理" colon={false}>
            <div className="icon-add" onClick={() => {
              const list = form.getFieldValue('badgeList') ?? [];
              form.setFieldsValue({ badgeList: [...list, { title: '', badge: undefined, expend: undefined }] });
            }}>
              <PlusOutlined style={{ fontSize: 16 }} />
              <span className="mar-l5">添加费用项</span>
            </div>
            <Form.List name="badgeList">
              {(fields, { remove }) => (
                <Table
                  className="budget-table"
                  bordered
                  size="small"
                  pagination={false}
                  dataSource={fields}
                  rowKey="key"
                  columns={[
                    {
                      title: '费用名',
                      render: (_, field) => (
                        <Form.Item name={[field.name, 'title']} rules={[{ required: true, message: '请选择' }]} noStyle>
                          <Select placeholder="选择费用名" style={{ width: 240 }}
                            options={BADGE_FEE_OPTIONS.map((t) => ({ value: t, label: t }))} />
                        </Form.Item>
                      ),
                    },
                    {
                      title: '预算(元)',
                      width: 180,
                      render: (_, field) => (
                        <Form.Item name={[field.name, 'badge']} noStyle>
                          <InputNumber style={{ width: '100%' }} min={0} />
                        </Form.Item>
                      ),
                    },
                    {
                      title: '实际支出(元)',
                      width: 180,
                      render: (_, field) => (
                        <Form.Item name={[field.name, 'expend']} noStyle>
                          <InputNumber style={{ width: '100%' }} min={0} />
                        </Form.Item>
                      ),
                    },
                    {
                      title: '操作',
                      width: 60,
                      align: 'center',
                      render: (_, field) =>
                        fields.length > 1 ? (
                          <DeleteOutlined
                            style={{ fontSize: 20, cursor: 'pointer', color: '#ff4d4f' }}
                            onClick={() => remove(field.name)}
                          />
                        ) : null,
                    },
                  ]}
                />
              )}
            </Form.List>
          </Form.Item>
        </Form>
      </Modal>

      {/* 查看弹窗 — 只读，含 reject_reason */}
      <Modal
        title="培训方案"
        open={viewOpen}
        onCancel={() => { setViewOpen(false); setTrainProgram(null); }}
        footer={null}
        width="60%"
        destroyOnClose
      >
        {trainProgram && (
          <div>
            <p className="mar-t10">
              <strong>方案标题：</strong>
              {trainProgram.title}
            </p>
            <p className="mar-t10">
              <strong>费用明细：</strong>
            </p>
            {trainProgram.reject_reason ? (
              <p className="mar-t10">
                <strong>企业拒绝理由：</strong>
                {trainProgram.reject_reason}
              </p>
            ) : null}
            <Table
              bordered
              size="small"
              pagination={false}
              rowKey="title"
              dataSource={trainProgram.badgeList ?? []}
              columns={viewColumns}
              style={{ marginTop: 10 }}
            />
            {trainProgram.file ? (
              <p className="mar-t10">
                <strong>方案详情：</strong>{' '}
                <a href={trainProgram.file} target="_blank" rel="noreferrer">
                  下载
                </a>
              </p>
            ) : null}
            {trainProgram.file && ['pdf', 'docx', 'pptx'].includes(trainProgram.fileType ?? '') ? (
              <div className="office-box mar-t10">
                {trainProgram.fileType === 'pdf' ? (
                  <iframe src={trainProgram.file} title="PDF预览" />
                ) : (
                  <iframe
                    title="文档预览"
                    src={`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(
                      window.location.origin + trainProgram.file,
                    )}`}
                  />
                )}
              </div>
            ) : null}
          </div>
        )}
      </Modal>
    </div>
  );
}
