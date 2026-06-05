/**
 * 班级人员 — 对齐 yun_shang classMembers.vue
 */
import { useMemo, useState } from 'react';
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Select,
  Space,
  Steps,
  Table,
  Upload,
  message,
} from 'antd';
import {
  EditOutlined,
  PictureOutlined,
  UploadOutlined,
  PlusOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { useAuthStore } from '../../stores/authStore';
import type {
  TrainInfoClassListItem,
  TrainInfoClassCreateReq,
  DemandPublicData,
  ClassStudent,
  ClassOther,
  ClassTask,
  SignUpPerson,
  TeacherBankItem,
} from '../../types/training';
import './TrainingClassPeoplePage.css';

const STEPS_ITEMS = [
  { title: '需求管理', icon: <EditOutlined /> },
  { title: '培训信息', icon: <UploadOutlined /> },
  { title: '班级人员', icon: <PictureOutlined /> },
  { title: '开班申报', icon: <EditOutlined /> },
  { title: '课表配置', icon: <UploadOutlined /> },
  { title: '毕业', icon: <PictureOutlined /> },
];

function resolveFileUrl(path?: string): string {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('/api/')) return path;
  return path.startsWith('/') ? path : `/${path}`;
}

function useLoginRole() {
  const loginRole = useAuthStore((s) => s.currentUser?.loginRole ?? 'org');
  return { loginRole, isOrg: loginRole === 'org' };
}

// ==================== 学员列表弹窗 ====================
function StudentListModal({
  open,
  onClose,
  classId,
  trainInfoId,
  isOrg,
}: {
  open: boolean;
  onClose: () => void;
  classId: number | null;
  trainInfoId: number | null;
  isOrg: boolean;
}) {
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [importOpen, setImportOpen] = useState(false);
  const [importByIdCardOpen, setImportByIdCardOpen] = useState(false);
  const [importKeyword, setImportKeyword] = useState('');
  const [selectedSignUpIds, setSelectedSignUpIds] = useState<number[]>([]);
  const [idCardText, setIdCardText] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 20;

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['class-students', classId, currentPage, pageSize],
    queryFn: () => trainApi.listClassStudents(classId!, currentPage, pageSize),
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
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.deleteClassStudents(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
    onError: () => message.error('删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; studentIds: number[] }) => trainApi.importStudents(p),
    onSuccess: (r) => {
      message.success(`导入成功 ${r.count} 人`);
      setImportOpen(false);
      setSelectedSignUpIds([]);
      refetch();
    },
    onError: () => message.error('导入失败'),
  });

  const importByIdCardMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; studentIds: string }) => trainApi.importStudentsByIdCard(p),
    onSuccess: (r) => {
      message.success(`导入成功 ${r.count} 人`);
      setImportByIdCardOpen(false);
      setIdCardText('');
      refetch();
    },
    onError: () => message.error('导入失败'),
  });

  const openEditStudent = (row: ClassStudent) => {
    editForm.setFieldsValue({
      id: row.id,
      realname: row.realname,
      phone: row.phone,
      id_card: row.id_card,
      normal_score: row.normal_score,
      is_leader: row.is_leader ?? 0,
      is_committee: row.is_committee ?? 0,
    });
    setEditOpen(true);
  };

  const renderIdCardPhoto = (row: ClassStudent) => {
    const src = resolveFileUrl(row.header);
    return (
      <div className="student-photo-cell">
        {src ? (
          <img src={src} alt="证件照" className="student-id-photo" />
        ) : (
          <span className="student-photo-empty">暂无</span>
        )}
      </div>
    );
  };

  const studentColumns: ColumnsType<ClassStudent> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60, align: 'center' },
    { title: '姓名', dataIndex: 'realname', key: 'realname', width: 100, align: 'left' },
    { title: '手机号', dataIndex: 'phone', key: 'phone', width: 120, align: 'center' },
    { title: '身份证号', dataIndex: 'id_card', key: 'id_card', width: 180, align: 'center' },
    {
      title: '平时分',
      dataIndex: 'normal_score',
      key: 'normal_score',
      width: 100,
      align: 'center',
      render: (v: number | undefined) => (v != null ? v : ''),
    },
    {
      title: '证件照',
      key: 'idcard_header',
      width: 180,
      align: 'center',
      render: (_: unknown, row: ClassStudent) => renderIdCardPhoto(row),
    },
    { title: '是否班长', dataIndex: 'is_leader_cn', key: 'is_leader_cn', width: 100, align: 'center' },
    { title: '是否学委', dataIndex: 'is_committee_cn', key: 'is_committee_cn', width: 100, align: 'center' },
    ...(isOrg
      ? [{
          title: '操作', key: 'act', width: 150, align: 'center' as const,
          render: (_: unknown, r: ClassStudent) => (
            <Space size="small">
              <Button type="link" size="small" className="link-action"
                onClick={() => openEditStudent(r)}>编辑</Button>
              <Button type="link" size="small" className="link-action-danger"
                onClick={() => Modal.confirm({
                  title: '友情提示',
                  content: '确定要删除该学员吗？',
                  onOk: () => deleteMu.mutate([r.id]),
                })}>删除</Button>
            </Space>
          ),
        }]
      : []),
  ];

  const signUpColumns: ColumnsType<SignUpPerson> = [
    { title: '姓名', dataIndex: 'name', align: 'center' },
    { title: '手机号', dataIndex: 'phone', align: 'center' },
  ];

  const handleImportSubmit = () => {
    if (!selectedSignUpIds.length) {
      message.error('请选择要导入的人员');
      return;
    }
    importMu.mutate({ traininfoId: trainInfoId!, classId: classId!, studentIds: selectedSignUpIds });
  };

  const handleImportByIdCardSubmit = () => {
    if (!idCardText.trim()) {
      message.error('请输入身份证号');
      return;
    }
    importByIdCardMu.mutate({ traininfoId: trainInfoId!, classId: classId!, studentIds: idCardText });
  };

  return (
    <>
      <Modal
        title="班级人员"
        open={open}
        onCancel={onClose}
        footer={null}
        width="92%"
        style={{ maxWidth: 1400 }}
        destroyOnClose
        rootClassName="class-student-list-modal"
        styles={{ body: { overflowX: 'auto', overflowY: 'visible', paddingTop: 12 } }}
      >
        {isOrg && (
          <div className="mar-b10">
            <Button onClick={() => setImportOpen(true)}>批量导入</Button>
            <Button onClick={() => setImportByIdCardOpen(true)} style={{ marginLeft: 8 }}>按身份证号导入</Button>
          </div>
        )}
        <div className="class-student-table-scroll">
          <Table
            className="class-student-table"
            rowKey="id"
            size="small"
            bordered
            loading={isLoading}
            columns={studentColumns}
            dataSource={data?.list ?? []}
            rowSelection={isOrg ? { columnWidth: 55 } : undefined}
            scroll={{ y: 500 }}
            pagination={false}
          />
        </div>
        <div className="student-pagination-wrap pagination-bar-wrap">
          <div className="pagination-bar">
            <Pagination
              current={currentPage}
              pageSize={pageSize}
              total={data?.total ?? 0}
              showSizeChanger={false}
              showTotal={(t) => `共 ${t} 条`}
              onChange={(p) => setCurrentPage(p)}
            />
          </div>
        </div>
      </Modal>

      <Modal
        title="班级人员编辑"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => saveMu.mutate(editForm.getFieldsValue())}
        confirmLoading={saveMu.isPending}
        okText="确定"
        cancelText="取消"
        width="35%"
        destroyOnClose
        className="training-class-people-page"
        wrapClassName="training-class-people-page"
      >
        <Form form={editForm} labelCol={{ style: { width: 80 } }} wrapperCol={{ span: 16 }}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="realname" label="姓名">
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item name="id_card" label="身份证号">
            <Input placeholder="请输入身份证号" />
          </Form.Item>
          <Form.Item name="normal_score" label="平时分">
            <Input type="number" placeholder="请输入平时分" />
          </Form.Item>
          <Form.Item name="is_leader" label="是否班长">
            <Select placeholder="请选择" options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} />
          </Form.Item>
          <Form.Item name="is_committee" label="是否学委">
            <Select placeholder="请选择" options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="班级人员导入"
        open={importOpen}
        onCancel={() => { setImportOpen(false); setSelectedSignUpIds([]); setImportKeyword(''); }}
        footer={null}
        width="35%"
        destroyOnClose
        className="training-class-people-page"
        wrapClassName="training-class-people-page"
      >
        <div className="mar-b10">
          <Input
            placeholder="关键搜索"
            value={importKeyword}
            onChange={(e) => setImportKeyword(e.target.value)}
            allowClear
            className="mar-r20"
            style={{ width: 200 }}
          />
          <Button type="primary" loading={importMu.isPending} onClick={handleImportSubmit}>确认导入</Button>
        </div>
        <Table
          rowKey="id"
          size="small"
          bordered
          columns={signUpColumns}
          dataSource={signUpData?.lists ?? []}
          rowSelection={{ columnWidth: 55, selectedRowKeys: selectedSignUpIds, onChange: (keys) => setSelectedSignUpIds(keys as number[]) }}
          scroll={{ y: 400 }}
          pagination={false}
        />
      </Modal>

      <Modal
        title="班级人员导入"
        open={importByIdCardOpen}
        onCancel={() => { setImportByIdCardOpen(false); setIdCardText(''); }}
        footer={null}
        maskClosable={false}
        width="35%"
        destroyOnClose
        className="training-class-people-page"
        wrapClassName="training-class-people-page"
      >
        <div>
          <Input.TextArea
            className="idcard-import-textarea"
            autoSize={{ minRows: 4, maxRows: 10 }}
            value={idCardText}
            onChange={(e) => setIdCardText(e.target.value)}
            placeholder="请输入身份证,一行一个"
          />
          <div className="idcard-import-actions">
            <Button type="primary" loading={importByIdCardMu.isPending} onClick={handleImportByIdCardSubmit}>确定</Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

// ==================== 教务/负责人弹窗 ====================
function StaffModal({
  open,
  onClose,
  classId,
  trainInfoId,
  tag,
  title: modalTitle,
  isOrg,
}: {
  open: boolean;
  onClose: () => void;
  classId: number | null;
  trainInfoId: number | null;
  tag: string;
  title: string;
  isOrg: boolean;
}) {
  const [editOpen, setEditOpen] = useState(false);
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
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.deleteClassOthers(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
    onError: () => message.error('删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; tag: string; ids: number[] }) => trainApi.importOthers(p),
    onSuccess: (r) => { message.success(`导入成功 ${r.count} 人`); setImportOpen(false); setSelectedTeacherIds([]); refetch(); },
    onError: () => message.error('导入失败'),
  });

  const columns: ColumnsType<ClassOther> = [
    { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
    { title: '姓名', dataIndex: 'realname' },
    { title: '手机号', dataIndex: 'phone', width: 120, align: 'center' },
    ...(isOrg
      ? [{
          title: '操作', key: 'act', width: 140, fixed: 'right' as const,
          render: (_: unknown, r: ClassOther) => (
            <Space size="small">
              <Button type="link" size="small" className="link-action" icon={<EditOutlined />}
                onClick={() => { editForm.setFieldsValue(r); setEditOpen(true); }}>编辑</Button>
              <Button type="link" size="small" className="link-action-danger"
                onClick={() => Modal.confirm({
                  title: '确定删除?',
                  onOk: () => deleteMu.mutate([r.id]),
                })}>删除</Button>
            </Space>
          ),
        }]
      : []),
  ];

  const teacherColumns: ColumnsType<TeacherBankItem> = [
    { title: '姓名', dataIndex: 'name' },
    { title: '手机号', dataIndex: 'phone', width: 200, align: 'center' },
  ];

  return (
    <>
      <Modal title={modalTitle} open={open} onCancel={onClose} footer={null} width="35%" destroyOnClose>
        {isOrg && (
          <Space style={{ marginBottom: 10 }}>
            {tag === 'teacher' ? (
              <Button onClick={() => setImportOpen(true)}>批量导入</Button>
            ) : (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => { editForm.resetFields(); setEditOpen(true); }}>新增</Button>
            )}
          </Space>
        )}
        <Table rowKey="id" size="small" bordered loading={isLoading} columns={columns}
          dataSource={data?.list ?? []} scroll={{ y: 500 }} pagination={false} />
      </Modal>

      <Modal title={modalTitle} open={editOpen} onOk={() => {
        const v = editForm.getFieldsValue();
        saveMu.mutate({
          ...v,
          id: v.id ?? 0,
          type: tag === 'teacher' ? 'staff' : 'inCharge',
          train_id: trainInfoId,
          classid: classId,
        });
      }} onCancel={() => setEditOpen(false)} confirmLoading={saveMu.isPending} width="35%" destroyOnClose>
        <Form form={editForm} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="realname" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="phone" label="手机号"><Input /></Form.Item>
          <Form.Item name="id_card" label="身份证号"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title="导入" open={importOpen}
        onOk={() => importMu.mutate({ traininfoId: trainInfoId!, classId: classId!, tag, ids: selectedTeacherIds })}
        onCancel={() => { setImportOpen(false); setSelectedTeacherIds([]); }}
        confirmLoading={importMu.isPending} width={500}>
        <Table rowKey="id" size="small" bordered columns={teacherColumns} dataSource={importData?.list ?? []}
          rowSelection={{ selectedRowKeys: selectedTeacherIds, onChange: (keys) => setSelectedTeacherIds(keys as number[]) }}
          scroll={{ y: 500 }} pagination={false} />
      </Modal>
    </>
  );
}

// ==================== 培训任务弹窗 ====================
function TrainTaskModal({
  open,
  onClose,
  classId,
  kejianOptions,
}: {
  open: boolean;
  onClose: () => void;
  classId: number | null;
  kejianOptions: { value: number; label: string }[];
}) {
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
    onError: () => message.error('保存失败'),
  });

  const columns: ColumnsType<ClassTask> = [
    { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
    { title: '课件名称', dataIndex: 'title' },
    { title: '日期', dataIndex: 'date', width: 160, align: 'center' },
    { title: '状态', dataIndex: 'status_cn', width: 100, align: 'center' },
    { title: '完成次数', dataIndex: 'times_cn', width: 100, align: 'center' },
    {
      title: '操作', key: 'act', width: 80, fixed: 'right',
      render: (_, r) => (
        <Button type="link" size="small" className="link-action" icon={<EditOutlined />} onClick={() => {
          setEditingId(r.id);
          editForm.setFieldsValue({ ck_id: r.ck_id, date: r.date ? dayjs(r.date) : undefined });
          setEditOpen(true);
        }}>编辑</Button>
      ),
    },
  ];

  return (
    <>
      <Modal title="培训任务编辑" open={open} onCancel={onClose} footer={null} width="60%" destroyOnClose>
        <Button type="primary" style={{ marginBottom: 10 }} onClick={() => {
          setEditingId(null);
          editForm.resetFields();
          setEditOpen(true);
        }}>新增</Button>
        <Table rowKey="id" size="small" bordered loading={isLoading} columns={columns}
          dataSource={data?.list ?? []} scroll={{ y: 400 }} pagination={false} />
      </Modal>

      <Modal title="培训任务信息" open={editOpen} onOk={() => {
        const v = editForm.getFieldsValue();
        const date = v.date ? (dayjs.isDayjs(v.date) ? v.date.format('YYYY-MM-DD') : v.date) : undefined;
        saveMu.mutate({ ...v, date, id: editingId ?? undefined, classId: classId! });
      }} onCancel={() => setEditOpen(false)} confirmLoading={saveMu.isPending} width="50%" destroyOnClose>
        <Form form={editForm} layout="vertical" labelCol={{ style: { width: 80 } }}>
          <Form.Item name="ck_id" label="课件名称" rules={[{ required: true, message: '请选课件名称' }]}>
            <Select showSearch filterOption={(input, option) => (option?.label as string || '').includes(input)}
              placeholder="请选课件名称" options={kejianOptions} />
          </Form.Item>
          <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

// ==================== 主页面 ====================
export function TrainingClassPeoplePage() {
  const queryClient = useQueryClient();
  const { isOrg } = useLoginRole();
  const [form] = Form.useForm<TrainInfoClassCreateReq & { kcbiaoFileDisplay?: string }>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);
  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [kcbiaoPreview, setKcbiaoPreview] = useState('');

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
    queryKey: ['info-classes', currentPage, pageSize, companyIdFilter, workTypeIdFilter],
    queryFn: () => trainApi.pageInfoClassList({
      page: currentPage,
      pageSize,
      companyId: companyIdFilter,
      workTypeId: workTypeIdFilter,
    }),
  });

  const saveMu = useMutation({
    mutationFn: (p: TrainInfoClassCreateReq) => trainApi.saveInfoClass(p),
    onSuccess: () => {
      message.success('保存成功');
      closeModal();
      queryClient.invalidateQueries({ queryKey: ['info-classes'] });
    },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.batchDeleteInfoClasses([id]),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['info-classes'] });
    },
    onError: () => message.error('删除失败'),
  });

  const workTypeOptions = useMemo(() => {
    const groups = (publicData as DemandPublicData | undefined)?.worktype_list ?? [];
    return groups.flatMap((g) =>
      (g.options ?? []).map((o) => ({
        value: o.id,
        label: o.title,
        group: g.title,
      })),
    );
  }, [publicData]);

  const companyOptions = useMemo(() => {
    return (publicData as DemandPublicData | undefined)?.company_list?.map((c) => ({
      value: c.id,
      label: c.title,
    })) ?? [];
  }, [publicData]);

  const trainInfoOptions = useMemo(() => {
    return (publicData as DemandPublicData | undefined)?.traininfo_list?.map((t) => ({
      value: t.id,
      label: t.title,
    })) ?? [];
  }, [publicData]);

  const kejianOptions = useMemo(() => {
    return (publicData as DemandPublicData | undefined)?.kejian_list?.map((k) => ({
      value: k.id,
      label: k.title,
    })) ?? [];
  }, [publicData]);

  const closeModal = () => {
    setModalOpen(false);
    setEditingId(null);
    setKcbiaoPreview('');
    form.resetFields();
  };

  const openCreate = () => {
    form.resetFields();
    setEditingId(null);
    setKcbiaoPreview('');
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
      kcbiaoFile: row.kcbiao_file,
      kcbiaoFileDisplay: row.kcbiao_file,
    });
    setKcbiaoPreview(row.kcbiao_src ?? resolveFileUrl(row.kcbiao_file));
    setEditingId(row.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const v = await form.validateFields();
    const payload: TrainInfoClassCreateReq = {
      id: v.id,
      title: v.title,
      trainInfoId: v.trainInfoId,
      nums: v.nums,
      dateStart: v.dateStart ? (dayjs.isDayjs(v.dateStart) ? v.dateStart.format('YYYY-MM-DD') : String(v.dateStart)) : undefined,
      dateEnd: v.dateEnd ? (dayjs.isDayjs(v.dateEnd) ? v.dateEnd.format('YYYY-MM-DD') : String(v.dateEnd)) : undefined,
      kcbiaoFile: v.kcbiaoFile,
    };
    saveMu.mutate(payload);
  };

  const kcbiaoUploadProps: UploadProps = {
    accept: '.xls,.xlsx',
    maxCount: 1,
    showUploadList: false,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      try {
        const result = await uploadFile(file as File, 'public');
        const url = `/api/files/${result.id}/download`;
        form.setFieldsValue({ kcbiaoFile: url, kcbiaoFileDisplay: url });
        setKcbiaoPreview(url);
        message.success('上传成功');
        onSuccess?.(result, file as unknown as XMLHttpRequest);
      } catch (err) {
        message.error('上传失败');
        onError?.(err as Error);
      }
    },
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

  const columns: ColumnsType<TrainInfoClassListItem> = [
    {
      title: '序号', key: 'index', width: 60, align: 'center',
      render: (_, __, i) => (currentPage - 1) * pageSize + i + 1,
    },
    { title: '标题', dataIndex: 'title', align: 'center' },
    { title: '公司', dataIndex: 'company_name', align: 'center' },
    { title: '工种', dataIndex: 'work_type_title', align: 'center' },
    { title: '人数', dataIndex: 'nums', width: 80, align: 'center' },
    { title: '创建时间', dataIndex: 'addtime_cn', align: 'center', render: (v, r) => v ?? (r.addtime ? dayjs.unix(r.addtime).format('YYYY-MM-DD HH:mm:ss') : '-') },
    {
      title: '操作', key: 'act', width: 220, align: 'center',
      render: (_, r) => (
        <Space size="small" wrap>
          <Button type="link" size="small" className="link-action" onClick={() => openSubDialog('students', r)}>学员列表</Button>
          <Button type="link" size="small" className="link-action" onClick={() => openSubDialog('staff', r)}>教务人员</Button>
          <Button type="link" size="small" className="link-action" onClick={() => openSubDialog('charge', r)}>负责人</Button>
          <Button type="link" size="small" className="link-action" onClick={() => openSubDialog('tasks', r)}>培训任务</Button>
          {isOrg && (
            <>
              <Button type="link" size="small" className="link-action" icon={<EditOutlined />} onClick={() => openEdit(r)}>编辑</Button>
              <Button type="link" size="small" className="link-action-danger" icon={<DeleteOutlined />}
                onClick={() => Modal.confirm({ title: '确定删除该班级?', onOk: () => deleteMu.mutate(r.id) })}>删除</Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const loginRole = useAuthStore((s) => s.currentUser?.loginRole);
  const hideCompanyFilter = loginRole === 'company';

  return (
    <div className="training-class-people-page">
      <Steps current={2} items={STEPS_ITEMS} style={{ maxWidth: '100%' }} />

      <div className="filter-bar">
        <Space wrap>
          {!hideCompanyFilter && (
            <Select allowClear showSearch placeholder="企业筛选" style={{ width: 200 }}
              value={companyIdFilter}
              onChange={(v) => { setCompanyIdFilter(v); setCurrentPage(1); }}
              options={companyOptions}
              filterOption={(input, option) => (option?.label as string || '').includes(input)} />
          )}
          <Select allowClear showSearch placeholder="请选择工种" style={{ width: 200 }}
            value={workTypeIdFilter}
            onChange={(v) => { setWorkTypeIdFilter(v); setCurrentPage(1); }}
            options={workTypeOptions.map((o) => ({ value: o.value, label: o.group ? `${o.group} / ${o.label}` : o.label }))} />
          <Button type="primary" onClick={() => { setCompanyIdFilter(undefined); setWorkTypeIdFilter(undefined); setCurrentPage(1); }}>重置</Button>
          {isOrg && (
            <Button type="primary" onClick={openCreate}>创建班级</Button>
          )}
        </Space>
      </div>

      <div className="table-wrap">
        <Table rowKey="id" bordered loading={isLoading} columns={columns}
          dataSource={listData?.list ?? []}
          scroll={{ x: 1200, y: 600 }}
          pagination={false} />
      </div>

      <div className="pagination-bar">
        <Pagination
          current={currentPage}
          pageSize={pageSize}
          total={listData?.total ?? 0}
          showTotal={(t) => `共 ${t} 条`}
          onChange={(p) => setCurrentPage(p)}
        />
      </div>

      <Modal title="班级信息" open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={saveMu.isPending} destroyOnClose width="50%">
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入标题" />
          </Form.Item>
          <Form.Item name="trainInfoId" label="选择培训信息" rules={[{ required: true, message: '请选择培训信息' }]}>
            <Select placeholder="请选择培训信息" options={trainInfoOptions} disabled={!!editingId} />
          </Form.Item>
          <Form.Item name="nums" label="班级人数">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入班级人数" />
          </Form.Item>
          <Form.Item name="dateStart" label="开始日期">
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item name="dateEnd" label="结束日期">
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item label="课程表">
            <div className="kcbiao-upload-row">
              <Form.Item name="kcbiaoFileDisplay" noStyle>
                <Input readOnly placeholder="请上传课表" style={{ flex: 1 }} />
              </Form.Item>
              <Upload {...kcbiaoUploadProps}>
                <Button>上传</Button>
              </Upload>
              {kcbiaoPreview && (
                <Button type="link" onClick={() => window.open(resolveFileUrl(kcbiaoPreview), '_blank')}>预览</Button>
              )}
            </div>
            <Form.Item name="kcbiaoFile" hidden><Input /></Form.Item>
          </Form.Item>
        </Form>
      </Modal>

      <StudentListModal open={studentModal} onClose={() => setStudentModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null} isOrg={isOrg} />
      <StaffModal open={staffModal} onClose={() => setStaffModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null}
        tag="teacher" title="教务人员" isOrg={isOrg} />
      <StaffModal open={chargeModal} onClose={() => setChargeModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null}
        tag="fuzhe" title="负责人" isOrg={isOrg} />
      <TrainTaskModal open={taskModal} onClose={() => setTaskModal(false)}
        classId={selectedClass?.id ?? null} kejianOptions={kejianOptions} />
    </div>
  );
}
