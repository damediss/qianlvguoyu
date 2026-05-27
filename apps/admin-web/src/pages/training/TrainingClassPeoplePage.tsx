import { useState } from 'react';
import { Button, Card, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Steps, Table, Typography, Upload, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, InboxOutlined, TeamOutlined, UserOutlined, SolutionOutlined, ScheduleOutlined, ImportOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import type { TrainInfoClassListItem, TrainInfoClassCreateReq, DemandPublicData, CompanyOption, SimpleItem, WorkTypeGroup, WorkTypeOption, ClassStudent, ClassOther, ClassTask, SignUpPerson, TeacherBankItem } from '../../types/training';

const { Dragger } = Upload;
const { TextArea } = Input;

// ==================== 学员列表弹窗 ====================
function StudentListModal({ open, onClose, classId, trainInfoId }: { open: boolean; onClose: () => void; classId: number | null; trainInfoId: number | null }) {
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [importOpen, setImportOpen] = useState(false);
  const [importByIdCardOpen, setImportByIdCardOpen] = useState(false);
  const [importKeyword, setImportKeyword] = useState('');
  const [selectedSignUpIds, setSelectedSignUpIds] = useState<number[]>([]);
  const [idCardText, setIdCardText] = useState('');

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['class-students', classId],
    queryFn: () => trainApi.listClassStudents(classId!, 1, 200),
    enabled: open && !!classId,
  });

  const { data: signUpData } = useQuery({
    queryKey: ['sign-up-self', trainInfoId, importKeyword],
    queryFn: () => trainApi.listSignUpSelf(trainInfoId!, importKeyword || undefined),
    enabled: importOpen && !!trainInfoId,
  });

  const saveMu = useMutation({
    mutationFn: (p: Record<string, unknown>) => trainApi.saveClassStudent(p),
    onSuccess: () => { message.success('保存成功'); setEditOpen(false); refetch(); },
  });

  const deleteMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.deleteClassStudents(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; studentIds: number[] }) => trainApi.importStudents(p),
    onSuccess: (r) => { message.success(`导入成功 ${r.count} 人`); setImportOpen(false); setSelectedSignUpIds([]); refetch(); },
  });

  const importByIdCardMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; studentIds: string }) => trainApi.importStudentsByIdCard(p),
    onSuccess: (r) => { message.success(`导入成功 ${r.count} 人`); setImportByIdCardOpen(false); setIdCardText(''); refetch(); },
  });

  const studentColumns: ColumnsType<ClassStudent> = [
    { title: '姓名', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    { title: '身份证', dataIndex: 'id_card', width: 180, ellipsis: true },
    { title: '班组长', dataIndex: 'is_leader_cn', width: 70 },
    { title: '班委', dataIndex: 'is_committee_cn', width: 70 },
    { title: '考试成绩', dataIndex: 'exam_score', width: 90 },
    { title: '实操成绩', dataIndex: 'real_score', width: 90 },
    { title: '平时成绩', dataIndex: 'normal_score', width: 90 },
    {
      title: '操作', key: 'act', width: 140,
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => { editForm.setFieldsValue(r); setEditOpen(true); }}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate([r.id])}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const signUpColumns: ColumnsType<SignUpPerson> = [
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
  ];

  return (
    <>
      <Modal title="学员列表" open={open} onCancel={onClose} footer={null} width={1000} destroyOnClose>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ImportOutlined />} onClick={() => setImportOpen(true)}>从报名导入</Button>
          <Button icon={<ImportOutlined />} onClick={() => setImportByIdCardOpen(true)}>按身份证导入</Button>
        </Space>
        <Table rowKey="id" size="small" loading={isLoading} columns={studentColumns} dataSource={data?.list ?? []}
          pagination={false} scroll={{ x: 900 }} />
      </Modal>

      {/* 编辑学员 */}
      <Modal title="编辑学员" open={editOpen} onOk={() => saveMu.mutate(editForm.getFieldsValue())} onCancel={() => setEditOpen(false)} confirmLoading={saveMu.isPending} destroyOnClose>
        <Form form={editForm} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="realname" label="姓名"><Input /></Form.Item>
          <Form.Item name="phone" label="手机号"><Input /></Form.Item>
          <Form.Item name="id_card" label="身份证号"><Input /></Form.Item>
          <Form.Item name="normal_score" label="平时成绩"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="is_leader" label="是否班组长"><Select options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} /></Form.Item>
          <Form.Item name="is_committee" label="是否班委"><Select options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} /></Form.Item>
        </Form>
      </Modal>

      {/* 从报名导入 */}
      <Modal title="从报名人员导入" open={importOpen} onOk={() => importMu.mutate({ traininfoId: trainInfoId!, classId: classId!, studentIds: selectedSignUpIds })}
        onCancel={() => { setImportOpen(false); setSelectedSignUpIds([]); }} confirmLoading={importMu.isPending} width={600}>
        <Input.Search placeholder="搜索姓名/手机号" value={importKeyword} onChange={e => setImportKeyword(e.target.value)} style={{ marginBottom: 16 }} />
        <Table rowKey="id" size="small" columns={signUpColumns} dataSource={signUpData?.lists ?? []}
          rowSelection={{ selectedRowKeys: selectedSignUpIds, onChange: keys => setSelectedSignUpIds(keys as number[]) }}
          pagination={false} />
      </Modal>

      {/* 按身份证号导入 */}
      <Modal title="按身份证号导入" open={importByIdCardOpen} onOk={() => importByIdCardMu.mutate({ traininfoId: trainInfoId!, classId: classId!, studentIds: idCardText })}
        onCancel={() => setImportByIdCardOpen(false)} confirmLoading={importByIdCardMu.isPending} width={500}>
        <Typography.Text type="secondary">每行一个身份证号</Typography.Text>
        <TextArea rows={8} value={idCardText} onChange={e => setIdCardText(e.target.value)} placeholder="每行一个身份证号" style={{ marginTop: 8 }} />
      </Modal>
    </>
  );
}

// ==================== 教务/负责人弹窗 ====================
function StaffModal({ open, onClose, classId, trainInfoId, tag, title: modalTitle }: { open: boolean; onClose: () => void; classId: number | null; trainInfoId: number | null; tag: string; title: string }) {
  const [editOpen, setEditOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ClassOther | null>(null);
  const [editForm] = Form.useForm();
  const [importOpen, setImportOpen] = useState(false);
  const [selectedTeacherIds, setSelectedTeacherIds] = useState<number[]>([]);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['class-other', classId, tag],
    queryFn: () => trainApi.listClassOthers(trainInfoId!, classId!, tag),
    enabled: open && !!classId && !!trainInfoId,
  });

  const { data: importData } = useQuery({
    queryKey: ['other-import-data', tag],
    queryFn: () => trainApi.listOtherImportData(tag),
    enabled: importOpen,
  });

  const saveMu = useMutation({
    mutationFn: (p: Record<string, unknown>) => trainApi.saveClassOther(p),
    onSuccess: () => { message.success('保存成功'); setEditOpen(false); refetch(); },
  });

  const deleteMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.deleteClassOthers(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; tag: string; ids: number[] }) => trainApi.importOthers(p),
    onSuccess: (r) => { message.success(`导入成功 ${r.count} 人`); setImportOpen(false); setSelectedTeacherIds([]); refetch(); },
  });

  const columns: ColumnsType<ClassOther> = [
    { title: '姓名', dataIndex: 'realname', width: 120 },
    { title: '手机号', dataIndex: 'phone', width: 140 },
    { title: '身份证', dataIndex: 'id_card', width: 180, ellipsis: true },
    {
      title: '操作', key: 'act', width: 120,
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => { setEditingRecord(r); editForm.setFieldsValue(r); setEditOpen(true); }}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate([r.id])}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const teacherColumns: ColumnsType<TeacherBankItem> = [
    { title: '姓名', dataIndex: 'name', width: 120 },
    { title: '手机号', dataIndex: 'phone', width: 140 },
  ];

  return (
    <>
      <Modal title={modalTitle} open={open} onCancel={onClose} footer={null} width={700} destroyOnClose>
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingRecord(null); editForm.resetFields(); setEditOpen(true); }}>添加</Button>
          {tag === 'teacher' && <Button icon={<ImportOutlined />} onClick={() => setImportOpen(true)}>从教师库导入</Button>}
        </Space>
        <Table rowKey="id" size="small" loading={isLoading} columns={columns} dataSource={data?.list ?? []}
          pagination={false} />
      </Modal>

      {/* 编辑 */}
      <Modal title={editingRecord ? '编辑' : '新增'} open={editOpen} onOk={() => {
        const v = editForm.getFieldsValue();
        saveMu.mutate({ ...v, id: editingRecord?.id ?? 0, type: tag === 'teacher' ? 'staff' : 'inCharge', train_id: trainInfoId, classid: classId });
      }} onCancel={() => setEditOpen(false)} confirmLoading={saveMu.isPending} destroyOnClose>
        <Form form={editForm} layout="vertical">
          <Form.Item name="realname" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="phone" label="手机号"><Input /></Form.Item>
          <Form.Item name="id_card" label="身份证号"><Input /></Form.Item>
        </Form>
      </Modal>

      {/* 从教师库导入 */}
      <Modal title="从教师库导入" open={importOpen}
        onOk={() => importMu.mutate({ traininfoId: trainInfoId!, classId: classId!, tag, ids: selectedTeacherIds })}
        onCancel={() => { setImportOpen(false); setSelectedTeacherIds([]); }} confirmLoading={importMu.isPending} width={500}>
        <Table rowKey="id" size="small" columns={teacherColumns} dataSource={importData?.list ?? []}
          rowSelection={{ selectedRowKeys: selectedTeacherIds, onChange: keys => setSelectedTeacherIds(keys as number[]) }}
          pagination={false} />
      </Modal>
    </>
  );
}

// ==================== 培训任务弹窗 ====================
function TrainTaskModal({ open, onClose, classId, kejianOptions }: { open: boolean; onClose: () => void; classId: number | null; kejianOptions: { value: number; label: string }[] }) {
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['class-tasks', classId],
    queryFn: () => trainApi.listClassTasks(classId!),
    enabled: open && !!classId,
  });

  const saveMu = useMutation({
    mutationFn: (p: { id?: number; classId?: number; ck_id?: number; date?: string }) => trainApi.saveClassTask(p),
    onSuccess: () => { message.success('保存成功'); setEditOpen(false); refetch(); },
  });

  const columns: ColumnsType<ClassTask> = [
    { title: '课件名称', dataIndex: 'title', width: 200 },
    { title: '日期', dataIndex: 'date', width: 120 },
    {
      title: '状态', dataIndex: 'status_cn', width: 80,
      render: (v: string) => v === '已完成' ? <Typography.Text type="success">{v}</Typography.Text> : <Typography.Text type="secondary">{v}</Typography.Text>,
    },
    {
      title: '操作', key: 'act', width: 100,
      render: (_, r) => (
        <Button type="link" size="small" icon={<EditOutlined />} onClick={() => {
          setEditingId(r.id);
          editForm.setFieldsValue({ ck_id: r.ck_id, date: r.date });
          setEditOpen(true);
        }}>编辑</Button>
      ),
    },
  ];

  return (
    <>
      <Modal title="培训任务" open={open} onCancel={onClose} footer={null} width={700} destroyOnClose>
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingId(null); editForm.resetFields(); setEditOpen(true); }}>新增任务</Button>
        </Space>
        <Table rowKey="id" size="small" loading={isLoading} columns={columns} dataSource={data?.list ?? []}
          pagination={false} />
      </Modal>

      <Modal title={editingId ? '编辑任务' : '新增任务'} open={editOpen}
        onOk={() => saveMu.mutate({ ...editForm.getFieldsValue(), id: editingId ?? undefined, classId: classId! })}
        onCancel={() => setEditOpen(false)} confirmLoading={saveMu.isPending} destroyOnClose>
        <Form form={editForm} layout="vertical">
          <Form.Item name="ck_id" label="课件" rules={[{ required: true, message: '请选择课件' }]}>
            <Select showSearch filterOption={(input, option) => (option?.label as string || '').includes(input)}
              placeholder="选择课件" options={kejianOptions} />
          </Form.Item>
          <Form.Item name="date" label="上课日期" rules={[{ required: true, message: '请选择日期' }]}>
            <Input placeholder="YYYY-MM-DD" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

// ==================== 主页面 ====================
export function TrainingClassPeoplePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainInfoClassCreateReq>();
  const [query, setQuery] = useState({ page: 1, pageSize: 20 });
  const [filterCompanyId, setFilterCompanyId] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  // Sub-dialog states
  const [studentModal, setStudentModal] = useState(false);
  const [staffModal, setStaffModal] = useState(false);
  const [chargeModal, setChargeModal] = useState(false);
  const [taskModal, setTaskModal] = useState(false);
  const [selectedClass, setSelectedClass] = useState<{ id: number; trainInfoId: number } | null>(null);

  const { data: publicData } = useQuery({
    queryKey: ['class-people-public-data'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'worktype', 'traininfo', 'kejian']),
    staleTime: 5 * 60 * 1000,
  });

  const { data: listData, isLoading } = useQuery({
    queryKey: ['info-classes', query, filterCompanyId],
    queryFn: () => trainApi.pageInfoClassList({ ...query, companyId: filterCompanyId }),
  });

  const saveMu = useMutation({
    mutationFn: (p: TrainInfoClassCreateReq) => trainApi.saveInfoClass(p),
    onSuccess: () => { message.success('保存成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['info-classes'] }); },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.batchDeleteInfoClasses([id]),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['info-classes'] }); },
    onError: () => message.error('删除失败'),
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };

  const openCreate = () => {
    form.resetFields();
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (row: TrainInfoClassListItem) => {
    form.setFieldsValue({
      id: row.id,
      title: row.title,
      trainInfoId: row.train_info_id,
      nums: row.nums,
      dateStart: row.date_start ? dayjs(row.date_start) : undefined,
      dateEnd: row.date_end ? dayjs(row.date_end) : undefined,
      fileTemps: row.file_temps,
      kcbiaoFile: row.kcbiao_file,
    });
    setEditingId(row.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const v = await form.validateFields();
    const payload = {
      ...v,
      dateStart: v.dateStart ? (typeof v.dateStart === 'string' ? v.dateStart : v.dateStart.format('YYYY-MM-DD')) : undefined,
      dateEnd: v.dateEnd ? (typeof v.dateEnd === 'string' ? v.dateEnd : v.dateEnd.format('YYYY-MM-DD')) : undefined,
    };
    saveMu.mutate(payload);
  };

  const openSubDialog = (type: string, row: TrainInfoClassListItem) => {
    setSelectedClass({ id: row.id, trainInfoId: row.train_info_id ?? 0 });
    switch (type) {
      case 'students': setStudentModal(true); break;
      case 'staff': setStaffModal(true); break;
      case 'charge': setChargeModal(true); break;
      case 'tasks': setTaskModal(true); break;
    }
  };

  // Resolve names from public data
  const pd = publicData as DemandPublicData | undefined;
  const companyOptions = pd?.company_list?.map((c: CompanyOption) => ({ value: c.id, label: c.title })) ?? [];
  const trainInfoOptions = pd?.traininfo_list?.map((t: SimpleItem) => ({ value: t.id, label: t.title })) ?? [];
  const kejianOptions = pd?.kejian_list?.map((k: SimpleItem) => ({ value: k.id, label: k.title })) ?? [];

  // File upload handlers
  const fileTempsProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.pdf,.docx,.xlsx',
    maxCount: 1,
    showUploadList: true,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      try {
        const result = await uploadFile(file as File, 'public');
        form.setFieldsValue({ fileTemps: `/api/files/${result.id}/download` });
        message.success('上传成功');
        onSuccess?.(result, file as unknown as XMLHttpRequest);
      } catch (err) {
        message.error('上传失败');
        onError?.(err as Error);
      }
    },
    onRemove: () => form.setFieldsValue({ fileTemps: undefined }),
  };

  const kcbiaoFileProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.pdf,.docx,.xlsx',
    maxCount: 1,
    showUploadList: true,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      try {
        const result = await uploadFile(file as File, 'public');
        form.setFieldsValue({ kcbiaoFile: `/api/files/${result.id}/download` });
        message.success('上传成功');
        onSuccess?.(result, file as unknown as XMLHttpRequest);
      } catch (err) {
        message.error('上传失败');
        onError?.(err as Error);
      }
    },
    onRemove: () => form.setFieldsValue({ kcbiaoFile: undefined }),
  };

  const columns: ColumnsType<TrainInfoClassListItem> = [
    {
      title: '序号', key: 'index', width: 60, align: 'center',
      render: (_, __, i) => (query.page - 1) * query.pageSize + i + 1,
    },
    { title: '班级名称', dataIndex: 'title', width: 160, ellipsis: true },
    { title: '关联培训', dataIndex: 'train_title', width: 180, ellipsis: true, render: (v: string) => v ?? '-' },
    { title: '企业', dataIndex: 'company_name', width: 140, render: (v: string) => v ?? '-' },
    { title: '工种', dataIndex: 'work_type_title', width: 120, render: (v: string) => v ?? '-' },
    { title: '人数', dataIndex: 'nums', width: 70, align: 'center' },
    { title: '开始日期', dataIndex: 'date_start', width: 110 },
    { title: '结束日期', dataIndex: 'date_end', width: 110 },
    {
      title: '操作', key: 'act', width: 320, fixed: 'right',
      render: (_, r) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<TeamOutlined />} onClick={() => openSubDialog('students', r)}>学员列表</Button>
          <Button type="link" size="small" icon={<SolutionOutlined />} onClick={() => openSubDialog('staff', r)}>教务人员</Button>
          <Button type="link" size="small" icon={<UserOutlined />} onClick={() => openSubDialog('charge', r)}>负责人</Button>
          <Button type="link" size="small" icon={<ScheduleOutlined />} onClick={() => openSubDialog('tasks', r)}>培训任务</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(r)}>编辑</Button>
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
          <Typography.Title level={4}>班级人员</Typography.Title>
          <Typography.Text type="secondary">管理培训班级信息，支持班级创建、学员分配、教务人员和培训任务管理</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Steps current={2} size="small"
          items={[
            { title: '企业需求' }, { title: '培训信息' }, { title: '班级人员' },
            { title: '培训方案' }, { title: '申报' }, { title: '补贴' },
          ]} />
      </Card>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select allowClear showSearch placeholder="企业筛选" style={{ width: 220 }}
            filterOption={(input, option) => (option?.label as string || '').includes(input)}
            value={filterCompanyId}
            onChange={v => { setFilterCompanyId(v); setQuery(prev => ({ ...prev, page: 1 })); }}
            options={companyOptions} />
          <Button onClick={() => { setFilterCompanyId(undefined); setQuery(prev => ({ ...prev, page: 1 })); }}>重置</Button>
        </Space>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增班级</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns}
          dataSource={listData?.list ?? []}
          scroll={{ x: 1300 }}
          pagination={{
            current: query.page, pageSize: query.pageSize, total: listData?.total ?? 0,
            showSizeChanger: false, showTotal: (t) => `共 ${t} 条`,
            position: ['bottomRight'],
            onChange: (p) => setQuery(prev => ({ ...prev, page: p })),
          }} />
      </Card>

      {/* Create/Edit Dialog */}
      <Modal title={editingId ? '编辑班级' : '新增班级'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={saveMu.isPending} destroyOnClose width={700}>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="title" label="班级名称" rules={[{ required: true, message: '请输入班级名称' }]}>
            <Input placeholder="请输入班级名称" />
          </Form.Item>
          <Form.Item name="trainInfoId" label="关联培训信息" rules={[{ required: true, message: '请选择关联培训信息' }]}>
            <Select showSearch placeholder="选择培训信息"
              filterOption={(input, option) => (option?.label as string || '').includes(input)}
              options={trainInfoOptions} disabled={!!editingId} />
          </Form.Item>
          <Form.Item name="nums" label="人数">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Space size={16}>
            <Form.Item name="dateStart" label="开始日期">
              <DatePicker placeholder="开始日期" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="dateEnd" label="结束日期">
              <DatePicker placeholder="结束日期" style={{ width: 200 }} />
            </Form.Item>
          </Space>

          <Form.Item label="文件模板">
            <Dragger {...fileTempsProps}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
            </Dragger>
          </Form.Item>

          <Form.Item label="课表文件">
            <Dragger {...kcbiaoFileProps}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
            </Dragger>
          </Form.Item>
        </Form>
      </Modal>

      {/* Sub-dialogs */}
      <StudentListModal open={studentModal} onClose={() => setStudentModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null} />
      <StaffModal open={staffModal} onClose={() => setStaffModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null}
        tag="teacher" title="教务人员" />
      <StaffModal open={chargeModal} onClose={() => setChargeModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null}
        tag="fuzhe" title="负责人" />
      <TrainTaskModal open={taskModal} onClose={() => setTaskModal(false)}
        classId={selectedClass?.id ?? null} kejianOptions={kejianOptions} />
    </div>
  );
}
