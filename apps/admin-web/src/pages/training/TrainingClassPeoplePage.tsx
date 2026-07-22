/**
 * 班级人员 — 对齐 yun_shang classMembers.vue
 */
import { useMemo, useState, useEffect } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  InboxOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import { ApiAuthImage } from '../../components/ApiAuthImage';
import { fileNameFromUrl } from '../../api/files';
import type {
  TrainInfoClassListItem,
  TrainInfoClassCreateReq,
  DemandPublicData,
  ClassStudent,
  ClassStudentPageResponse,
  ClassOther,
  ClassTask,
  SignUpPerson,
  TeacherBankItem,
} from '../../types/training';
import './TrainingClassPeoplePage.css';
import { TrainingProcessSteps } from './TrainingProcessSteps';

const { Dragger } = Upload;

function resolveFileUrl(path?: string): string {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('/api/')) return path;
  return path.startsWith('/') ? path : `/${path}`;
}

/** 兼容接口 snake_case / camelCase 字段 */
function normalizeClassStudent(raw: Record<string, unknown>): ClassStudent {
  const pick = (...keys: string[]) => {
    for (const key of keys) {
      const v = raw[key];
      if (v !== undefined && v !== null && v !== '') return v;
    }
    return undefined;
  };
  return {
    id: Number(pick('id')),
    realname: String(pick('realname', 'name') ?? ''),
    phone: pick('phone') != null ? String(pick('phone')) : undefined,
    train_id: pick('train_id', 'trainId') != null ? Number(pick('train_id', 'trainId')) : undefined,
    train_class_id: pick('train_class_id', 'trainClassId') != null ? Number(pick('train_class_id', 'trainClassId')) : undefined,
    id_card: pick('id_card', 'idCard') != null ? String(pick('id_card', 'idCard')) : undefined,
    is_leader: pick('is_leader', 'isLeader') != null ? Number(pick('is_leader', 'isLeader')) : undefined,
    is_leader_cn: pick('is_leader_cn', 'isLeaderCn') != null ? String(pick('is_leader_cn', 'isLeaderCn')) : undefined,
    is_committee: pick('is_committee', 'isCommittee') != null ? Number(pick('is_committee', 'isCommittee')) : undefined,
    is_committee_cn: pick('is_committee_cn', 'isCommitteeCn') != null ? String(pick('is_committee_cn', 'isCommitteeCn')) : undefined,
    normal_score: pick('normal_score', 'normalScore') != null ? Number(pick('normal_score', 'normalScore')) : undefined,
    header: pick('header', 'idcardHeader', 'idcard_header') != null ? String(pick('header', 'idcardHeader', 'idcard_header')) : undefined,
  };
}

function normalizeClassStudentList(data?: ClassStudentPageResponse | { lists?: ClassStudent[]; list?: ClassStudent[] }) {
  const rows = (data?.list ?? (data as { lists?: ClassStudent[] })?.lists ?? []) as unknown as Record<string, unknown>[];
  return rows.map((row) => normalizeClassStudent(row));
}

// ==================== 班级人员弹窗（对齐 yun_shang classMembers.vue） ====================
function StudentListModal({
  open,
  onClose,
  classId,
  trainInfoId,
}: {
  open: boolean;
  onClose: () => void;
  classId: number | null;
  trainInfoId: number | null;
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

  useEffect(() => {
    if (open) {
      setCurrentPage(1);
    }
  }, [open, classId]);

  const normalizedClassId = classId != null ? Number(classId) : null;

  const { data, isLoading, refetch, isError } = useQuery({
    queryKey: ['class-students', normalizedClassId, currentPage, pageSize],
    queryFn: () => trainApi.listClassStudents(normalizedClassId!, currentPage, pageSize),
    enabled: open && normalizedClassId != null && !Number.isNaN(normalizedClassId),
  });

  const { data: signUpData } = useQuery({
    queryKey: ['sign-up-self', trainInfoId, importKeyword],
    queryFn: () => trainApi.listSignUpSelf(trainInfoId!, importKeyword || undefined),
    enabled: importOpen && !!trainInfoId,
  });

  const saveMu = useMutation({
    mutationFn: (p: Record<string, unknown>) => trainApi.saveClassStudent(p),
    onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.deleteClassStudents(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
    onError: () => message.error('删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; studentIds: number[] }) => trainApi.importStudents(p),
    onSuccess: () => {
      message.success('导入成功');
      setImportOpen(false);
      setSelectedSignUpIds([]);
      refetch();
    },
    onError: () => message.error('导入失败'),
  });

  const importByIdCardMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; studentIds: string }) => trainApi.importStudentsByIdCard(p),
    onSuccess: () => {
      message.success('导入成功');
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
      is_leader: String(row.is_leader ?? 0),
      is_committee: String(row.is_committee ?? 0),
    });
    setEditOpen(true);
  };

  const formatYesNo = (cn?: string, val?: number) => cn ?? (val === 1 ? '是' : '否');

  const formatNormalScore = (score?: number) => {
    if (score == null || Number.isNaN(Number(score))) return '0.00';
    return Number(score).toFixed(2);
  };

  const renderIdCardPhoto = (row: ClassStudent) => {
    const url = row.header?.trim();
    if (!url) return <div className="student-photo-cell" />;
    return (
      <div className="student-photo-cell">
        <ApiAuthImage
          url={url}
          alt="证件照"
          className="student-id-photo"
          width={72}
          height={96}
          style={{ objectFit: 'cover', borderRadius: 2 }}
          preview={{ mask: '预览' }}
        />
      </div>
    );
  };

  const handleDeleteStudent = (row: ClassStudent) => {
    Modal.confirm({
      title: '友情提示',
      content: '确定要删除该学员吗？',
      onOk: () => deleteMu.mutate([row.id]),
    });
  };

  const studentColumns: ColumnsType<ClassStudent> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 72, align: 'center' },
    { title: '姓名', dataIndex: 'realname', key: 'realname', width: 100, align: 'left' },
    { title: '手机号', dataIndex: 'phone', key: 'phone', width: 124, align: 'center' },
    { title: '身份证号', dataIndex: 'id_card', key: 'id_card', width: 188, align: 'center' },
    {
      title: '平时分',
      dataIndex: 'normal_score',
      key: 'normal_score',
      width: 88,
      align: 'center',
      render: (v: number | undefined) => formatNormalScore(v),
    },
    {
      title: '证件照',
      key: 'idcard_header',
      width: 100,
      align: 'center',
      render: (_: unknown, row: ClassStudent) => renderIdCardPhoto(row),
    },
    {
      title: '是否班长',
      key: 'is_leader_cn',
      width: 88,
      align: 'center',
      render: (_: unknown, row: ClassStudent) => formatYesNo(row.is_leader_cn, row.is_leader),
    },
    {
      title: '是否学委',
      key: 'is_committee_cn',
      width: 88,
      align: 'center',
      render: (_: unknown, row: ClassStudent) => formatYesNo(row.is_committee_cn, row.is_committee),
    },
    {
      title: '操作',
      key: 'act',
      width: 120,
      align: 'center',
      fixed: 'right',
      render: (_: unknown, row: ClassStudent) => (
        <div className="student-row-actions">
          <button type="button" className="student-action-link" onClick={() => openEditStudent(row)}>
            编辑
          </button>
          <button
            type="button"
            className="student-action-link student-action-link-danger"
            onClick={() => handleDeleteStudent(row)}
          >
            删除
          </button>
        </div>
      ),
    },
  ];

  const signUpColumns: ColumnsType<SignUpPerson> = [
    { title: '姓名', dataIndex: 'name', align: 'center' },
    { title: '手机号', dataIndex: 'phone', align: 'center' },
  ];

  const studentRows = useMemo(
    () => normalizeClassStudentList(data),
    [data],
  );

  const handleImportSubmit = () => {
    if (!selectedSignUpIds.length) {
      message.error('请选择要导入的人员');
      return;
    }
    if (!trainInfoId || normalizedClassId == null) {
      message.error('班级未关联培训信息');
      return;
    }
    importMu.mutate({ traininfoId: trainInfoId, classId: normalizedClassId, studentIds: selectedSignUpIds });
  };

  const handleImportByIdCardSubmit = () => {
    if (!idCardText.trim()) {
      message.error('请输入身份证号');
      return;
    }
    if (!trainInfoId || normalizedClassId == null) {
      message.error('班级未关联培训信息');
      return;
    }
    importByIdCardMu.mutate({ traininfoId: trainInfoId, classId: normalizedClassId, studentIds: idCardText });
  };

  return (
    <>
      <Modal
        title="班级人员"
        open={open}
        onCancel={onClose}
        footer={null}
        width="88%"
        centered
        destroyOnHidden
        rootClassName="class-student-list-modal"
        styles={{ body: { padding: '12px 16px 16px', maxHeight: '78vh', overflow: 'hidden' } }}
      >
        <div className="student-toolbar">
          <Button className="student-toolbar-btn" onClick={() => setImportOpen(true)}>
            批量导入
          </Button>
          <Button className="student-toolbar-btn" onClick={() => setImportByIdCardOpen(true)}>
            按身份证号导入
          </Button>
        </div>
        <div className="class-student-table-scroll">
          <Table
            className="class-student-table"
            rowKey="id"
            bordered
            size="middle"
            loading={isLoading}
            columns={studentColumns}
            dataSource={studentRows}
            rowSelection={{ columnWidth: 48 }}
            scroll={{ x: 1100, y: 480 }}
            pagination={false}
            locale={{ emptyText: isError ? '加载失败，请关闭后重试' : '暂无数据' }}
          />
        </div>
        <Pagination
          className="student-pagination"
          current={currentPage}
          pageSize={pageSize}
          total={data?.total ?? 0}
          showSizeChanger={false}
          showQuickJumper
          showTotal={(t) => `共 ${t} 条`}
          onChange={(p) => setCurrentPage(p)}
        />
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
        destroyOnHidden
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
            <Select placeholder="请选择" options={[{ value: '1', label: '是' }, { value: '0', label: '否' }]} />
          </Form.Item>
          <Form.Item name="is_committee" label="是否学委">
            <Select placeholder="请选择" options={[{ value: '1', label: '是' }, { value: '0', label: '否' }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="班级人员导入"
        open={importOpen}
        onCancel={() => { setImportOpen(false); setSelectedSignUpIds([]); setImportKeyword(''); }}
        footer={null}
        width="35%"
        destroyOnHidden
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
        destroyOnHidden
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

// ==================== 教务/负责人弹窗（对齐 yun_shang classMembers.vue） ====================
function StaffModal({
  open,
  onClose,
  classId,
  trainInfoId,
  tag,
  title: modalTitle,
}: {
  open: boolean;
  onClose: () => void;
  classId: number | null;
  trainInfoId: number | null;
  tag: string;
  title: string;
}) {
  const [editOpen, setEditOpen] = useState(false);
  const [editTitle, setEditTitle] = useState('');
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
    onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.deleteClassOthers(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
    onError: () => message.error('删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: number; classId: number; tag: string; ids: number[] }) => trainApi.importOthers(p),
    onSuccess: () => { message.success('导入成功'); setImportOpen(false); setSelectedTeacherIds([]); refetch(); },
    onError: () => message.error('导入失败'),
  });

  const openAdd = () => {
    editForm.resetFields();
    editForm.setFieldsValue({
      id: 0,
      train_id: trainInfoId,
      classid: classId,
      type: 'inCharge',
    });
    setEditTitle(modalTitle);
    setEditOpen(true);
  };

  const openEdit = (row: ClassOther) => {
    const titleMap: Record<string, string> = { teacher: '教务人员', fuzhe: '负责人' };
    setEditTitle(`${titleMap[tag] ?? modalTitle}编辑`);
    editForm.setFieldsValue({
      id: row.id,
      train_id: row.train_id,
      classid: row.train_class_id,
      realname: row.realname,
      phone: row.phone,
      id_card: row.id_card,
      type: row.type,
    });
    setEditOpen(true);
  };

  const handleDeleteStaff = (row: ClassOther) => {
    Modal.confirm({
      title: '友情提示',
      content: '确定要删除该人员吗？',
      onOk: () => deleteMu.mutate([row.id]),
    });
  };

  const columns: ColumnsType<ClassOther> = [
    { title: 'ID', dataIndex: 'id', width: 72, align: 'center' },
    { title: '姓名', dataIndex: 'realname', align: 'left' },
    { title: '手机号', dataIndex: 'phone', width: 140, align: 'center' },
    {
      title: '操作',
      key: 'act',
      width: 120,
      align: 'center',
      render: (_: unknown, row: ClassOther) => (
        <div className="staff-row-actions">
          <button type="button" className="staff-action-link" onClick={() => openEdit(row)}>编辑</button>
          <button type="button" className="staff-action-link staff-action-link-danger" onClick={() => handleDeleteStaff(row)}>
            删除
          </button>
        </div>
      ),
    },
  ];

  const teacherColumns: ColumnsType<TeacherBankItem> = [
    { title: '姓名', dataIndex: 'name', align: 'left' },
    { title: '手机号', dataIndex: 'phone', width: 200, align: 'center' },
  ];

  return (
    <>
      <Modal
        title={modalTitle}
        open={open}
        onCancel={onClose}
        footer={null}
        width="52%"
        centered
        destroyOnHidden
        rootClassName="class-staff-modal"
        styles={{ body: { padding: '12px 16px 16px' } }}
      >
        <div className="staff-toolbar">
          {tag === 'teacher' ? (
            <Button className="staff-toolbar-btn" onClick={() => setImportOpen(true)}>
              批量导入
            </Button>
          ) : (
            <Button className="staff-toolbar-btn" onClick={openAdd}>
              新增
            </Button>
          )}
        </div>
        <Table
          className="class-staff-table"
          rowKey="id"
          bordered
          size="middle"
          loading={isLoading}
          columns={columns}
          dataSource={data?.list ?? []}
          scroll={{ y: 420 }}
          pagination={false}
          locale={{ emptyText: '暂无数据' }}
        />
      </Modal>

      <Modal
        title={editTitle || modalTitle}
        open={editOpen}
        onOk={() => saveMu.mutate(editForm.getFieldsValue())}
        onCancel={() => { setEditOpen(false); setEditTitle(''); editForm.resetFields(); }}
        confirmLoading={saveMu.isPending}
        okText="确定"
        cancelText="取消"
        width="35%"
        destroyOnHidden
        rootClassName="class-staff-modal"
      >
        <Form form={editForm} labelCol={{ style: { width: 80 } }} wrapperCol={{ span: 16 }}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="train_id" hidden><Input /></Form.Item>
          <Form.Item name="classid" hidden><Input /></Form.Item>
          <Form.Item name="type" hidden><Input /></Form.Item>
          <Form.Item name="realname" label="姓名">
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item name="id_card" label="身份证号">
            <Input placeholder="请输入身份证号" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量导入"
        open={importOpen}
        onCancel={() => { setImportOpen(false); setSelectedTeacherIds([]); }}
        footer={null}
        maskClosable={false}
        width={520}
        destroyOnHidden
        rootClassName="class-staff-modal"
      >
        <div className="staff-import-actions">
          <Button
            type="primary"
            loading={importMu.isPending}
            onClick={() => {
              if (!selectedTeacherIds.length) {
                message.error('请选择要导入的人员');
                return;
              }
              importMu.mutate({ traininfoId: trainInfoId!, classId: classId!, tag, ids: selectedTeacherIds });
            }}
          >
            确认导入
          </Button>
        </div>
        <Table
          className="class-staff-table"
          rowKey="id"
          bordered
          size="middle"
          columns={teacherColumns}
          dataSource={importData?.list ?? []}
          rowSelection={{
            columnWidth: 48,
            selectedRowKeys: selectedTeacherIds,
            onChange: (keys) => setSelectedTeacherIds(keys as number[]),
          }}
          scroll={{ y: 420 }}
          pagination={false}
          locale={{ emptyText: '暂无数据' }}
        />
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
    onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
    onError: () => message.error('保存失败'),
  });

  const columns: ColumnsType<ClassTask> = [
    { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
    { title: '课件名称', dataIndex: 'title', align: 'left' },
    { title: '日期', dataIndex: 'date', width: 160, align: 'center' },
    { title: '状态', dataIndex: 'status_cn', width: 160, align: 'center' },
    { title: '完成次数', dataIndex: 'times_cn', width: 160, align: 'center' },
    {
      title: '操作', key: 'act', width: 150, fixed: 'right', align: 'center',
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" className="link-action" onClick={() => {
            setEditingId(r.id);
            editForm.setFieldsValue({ ck_id: r.ck_id, date: r.date ? dayjs(r.date) : undefined });
            setEditOpen(true);
          }}>编辑</Button>
          <Button type="link" size="small" />
        </Space>
      ),
    },
  ];

  return (
    <>
      <Modal title="培训任务编辑" open={open} onCancel={onClose} footer={null} width="60%" destroyOnHidden>
        <div className="mar-b10">
          <Button onClick={() => {
            setEditingId(null);
            editForm.resetFields();
            setEditOpen(true);
          }}>新增</Button>
        </div>
        <Table rowKey="id" bordered loading={isLoading} columns={columns}
          dataSource={data?.list ?? []} scroll={{ y: 400 }} pagination={false} />
      </Modal>

      <Modal
        title="培训任务信息"
        open={editOpen}
        onOk={() => {
          const v = editForm.getFieldsValue();
          const date = v.date ? (dayjs.isDayjs(v.date) ? v.date.format('YYYY-MM-DD') : v.date) : undefined;
          saveMu.mutate({ ...v, date, id: editingId ?? undefined, classId: classId! });
        }}
        onCancel={() => setEditOpen(false)}
        confirmLoading={saveMu.isPending}
        okText="确定"
        cancelText="取消"
        width="50%"
        destroyOnHidden
      >
        <Form form={editForm} labelCol={{ style: { width: 80 } }} wrapperCol={{ span: 16 }}>
          <Form.Item name="ck_id" label="课件名称">
            <Select showSearch filterOption={(input, option) => (option?.label as string || '').includes(input)}
              placeholder="请选课件名称" allowClear options={kejianOptions} />
          </Form.Item>
          <Form.Item name="date" label="日期">
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

// ==================== 主页面 ====================
export function TrainingClassPeoplePage() {
  const processStep = 3;
  const queryClient = useQueryClient();
  const [form] = Form.useForm<{
    id?: number;
    title?: string;
    trainInfoId?: number;
    nums?: number;
    dateStart?: dayjs.Dayjs;
    dateEnd?: dayjs.Dayjs;
    kcbiaoFile?: string;
    kcbiaoFileDisplay?: string;
  }>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);
  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [kcbiaoSrc, setKcbiaoSrc] = useState('');

  const [uploadOpen, setUploadOpen] = useState(false);
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);

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
      message.success('成功');
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

  const workTypeGroupedOptions = useMemo(() => {
    return (publicData as DemandPublicData | undefined)?.worktype_list?.map((g) => ({
      label: g.title,
      options: (g.options ?? []).map((o) => ({ value: o.id, label: o.title })),
    })) ?? [];
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
    setKcbiaoSrc('');
    form.resetFields();
  };

  const openCreate = () => {
    form.resetFields();
    setKcbiaoSrc('');
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
    setKcbiaoSrc(row.kcbiao_src ?? resolveFileUrl(row.kcbiao_file));
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const v = form.getFieldsValue();
    if (!v.title) {
      message.error('请填写班级名称');
      return;
    }
    if (!v.trainInfoId) {
      message.error('请选择班级课程');
      return;
    }
    if (!v.nums) {
      message.error('请填写班级人数');
      return;
    }
    if (!v.dateStart) {
      message.error('请选择班级开始时间');
      return;
    }
    if (!v.dateEnd) {
      message.error('请选择班级结束时间');
      return;
    }
    const payload: TrainInfoClassCreateReq = {
      id: v.id,
      title: v.title,
      trainInfoId: v.trainInfoId,
      nums: v.nums,
      dateStart: dayjs.isDayjs(v.dateStart) ? v.dateStart.format('YYYY-MM-DD') : String(v.dateStart),
      dateEnd: dayjs.isDayjs(v.dateEnd) ? v.dateEnd.format('YYYY-MM-DD') : String(v.dateEnd),
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
        message.success('上传成功');
        setUploadOpen(false);
        onSuccess?.(result, file as unknown as XMLHttpRequest);
      } catch (err) {
        message.error('上传失败');
        onError?.(err as Error);
      }
    },
  };

  const openPreview = (src?: string) => {
    const url = resolveFileUrl(src);
    if (!url) return;
    setPreviewTarget({
      fileUrl: url,
      fileName: fileNameFromUrl(url),
    });
  };

  const handleFilterChange = () => {
    setCurrentPage(1);
  };

  const handleReset = () => {
    setCompanyIdFilter(undefined);
    setWorkTypeIdFilter(undefined);
    setCurrentPage(1);
  };

  const openSubDialog = (type: string, row: TrainInfoClassListItem) => {
    setSelectedClass({
      id: Number(row.id),
      trainInfoId: Number(row.train_info_id ?? 0),
    });
    switch (type) {
      case 'students': setStudentModal(true); break;
      case 'staff': setStaffModal(true); break;
      case 'charge': setChargeModal(true); break;
      case 'tasks': setTaskModal(true); break;
    }
  };

  const formatCreateTime = (row: TrainInfoClassListItem) => {
    if (row.addtime_cn) return row.addtime_cn;
    if (row.addtime) return dayjs.unix(row.addtime).format('YYYY-MM-DD HH:mm:ss');
    return '-';
  };

  const handleDeleteClass = (row: TrainInfoClassListItem) => {
    Modal.confirm({
      title: '友情提示',
      content: '确定要删除该班级吗？',
      onOk: () => deleteMu.mutate(row.id),
    });
  };

  const renderRowActions = (row: TrainInfoClassListItem) => (
    <div className="class-row-actions">
      <div className="class-row-actions-line">
        <button type="button" className="class-action-link" onClick={() => openSubDialog('students', row)}>学员列表</button>
        <button type="button" className="class-action-link" onClick={() => openSubDialog('staff', row)}>教务人员</button>
        <button type="button" className="class-action-link" onClick={() => openSubDialog('charge', row)}>负责人</button>
      </div>
      <div className="class-row-actions-line">
        <button type="button" className="class-action-link" onClick={() => openSubDialog('tasks', row)}>培训任务</button>
        <button type="button" className="class-action-link" onClick={() => openEdit(row)}>编辑</button>
        <button type="button" className="class-action-link class-action-link-danger" onClick={() => handleDeleteClass(row)}>删除</button>
      </div>
    </div>
  );

  const columns: ColumnsType<TrainInfoClassListItem> = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      align: 'center',
      render: (_, __, i) => (currentPage - 1) * pageSize + i + 1,
    },
    { title: '班级名称', dataIndex: 'title', align: 'left', ellipsis: true },
    { title: '公司', dataIndex: 'company_name', align: 'left', ellipsis: true },
    { title: '工种', dataIndex: 'work_type_title', align: 'center', width: 140 },
    { title: '人数', dataIndex: 'nums', width: 80, align: 'center' },
    {
      title: '创建时间',
      key: 'createTime',
      width: 174,
      align: 'center',
      render: (_, row) => formatCreateTime(row),
    },
    {
      title: '操作',
      key: 'act',
      width: 240,
      align: 'center',
      fixed: 'right',
      render: (_, row) => renderRowActions(row),
    },
  ];

  return (
    <div className="training-class-people-page training-page">
      <section className="page-heading">
        <Typography.Title level={4}>班级人员</Typography.Title>
        <Typography.Text type="secondary">管理培训班级人员，维护学员、班委及跟班任务</Typography.Text>
      </section>

      <TrainingProcessSteps current={processStep} />

      <Card size="small" style={{ marginBottom: 16 }}>
      <div className="search-filter-bar">
        <div className="filter-actions">
          <Select
            className="filter-select"
            allowClear
            showSearch
            placeholder="企业筛选"
            value={companyIdFilter}
            onChange={(v) => {
              setCompanyIdFilter(v);
              handleFilterChange();
            }}
            options={companyOptions}
            optionFilterProp="label"
          />
          <Select
            className="filter-select"
            allowClear
            showSearch
            placeholder="请选择工种"
            value={workTypeIdFilter}
            onChange={(v) => {
              setWorkTypeIdFilter(v);
              handleFilterChange();
            }}
            options={workTypeGroupedOptions}
            optionFilterProp="label"
          />
          <Button type="primary" onClick={handleReset}>重置</Button>
          <Button type="primary" onClick={openCreate}>创建班级</Button>
        </div>
      </div>
      </Card>

      <Table
        className="class-people-table"
        rowKey="id"
        bordered
        size="middle"
        loading={isLoading}
        columns={columns}
        dataSource={listData?.list ?? []}
        scroll={{ x: 1100, y: 600 }}
        pagination={false}
      />

      <Pagination
        className="class-people-pagination"
        current={currentPage}
        pageSize={pageSize}
        total={listData?.total ?? 0}
        showSizeChanger={false}
        showQuickJumper
        showTotal={(t) => `共 ${t} 条`}
        onChange={(p) => setCurrentPage(p)}
      />

      <Modal
        title="班级信息"
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={closeModal}
        confirmLoading={saveMu.isPending}
        okText="确定"
        cancelText="取消"
        destroyOnHidden
        width="50%"
      >
        <Form form={form} labelCol={{ style: { width: 120 } }} wrapperCol={{ span: 16 }}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="title" label="班级名称">
            <Input placeholder="请输入班级名称" />
          </Form.Item>
          <Form.Item name="trainInfoId" label="选择培训信息">
            <Select placeholder="请选择培训信息" options={trainInfoOptions} />
          </Form.Item>
          <Form.Item name="nums" label="班级人数">
            <Input type="number" placeholder="请输入班级人数" />
          </Form.Item>
          <Form.Item name="dateStart" label="开始日期">
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" placeholder="选择开始日期" />
          </Form.Item>
          <Form.Item name="dateEnd" label="结束日期">
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" placeholder="选择结束日期" />
          </Form.Item>
          <Form.Item label="课程表">
            <Form.Item noStyle shouldUpdate={(p, c) => p.kcbiaoFileDisplay !== c.kcbiaoFileDisplay || p.kcbiaoFile !== c.kcbiaoFile}>
              {() => {
                const fileVal = form.getFieldValue('kcbiaoFileDisplay');
                return (
                  <div className="kcbiao-upload-row">
                    <Form.Item name="kcbiaoFileDisplay" noStyle>
                      <Input readOnly placeholder="请上传课表" style={{ flex: 1 }} />
                    </Form.Item>
                    <Button onClick={() => setUploadOpen(true)}>上传</Button>
                    {fileVal ? (
                      <Button type="link" style={{ marginLeft: 20 }} onClick={() => openPreview(kcbiaoSrc || form.getFieldValue('kcbiaoFile'))}>
                        预览
                      </Button>
                    ) : null}
                  </div>
                );
              }}
            </Form.Item>
            <Form.Item name="kcbiaoFile" hidden><Input /></Form.Item>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="上传文件"
        open={uploadOpen}
        width={500}
        onCancel={() => setUploadOpen(false)}
        footer={null}
        maskClosable={false}
        destroyOnHidden
      >
        <Dragger {...kcbiaoUploadProps}>
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p className="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
          <p style={{ color: '#ccc', marginTop: 10 }}>提示:上传过程中,请不要做任何操作</p>
          <p className="ant-upload-hint">只能上传.xls,.xlsx文件</p>
        </Dragger>
      </Modal>

      <FilePreviewModal
        open={previewTarget != null}
        target={previewTarget}
        onClose={() => setPreviewTarget(null)}
      />

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
