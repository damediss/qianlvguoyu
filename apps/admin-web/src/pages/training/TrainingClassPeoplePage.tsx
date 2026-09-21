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
  InputNumber,
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
  EditOutlined,
  InboxOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';
import { apiClient, unwrap } from '../../api/client';
import { uploadFile } from '../../api/knowledge';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import { AuthImage } from '../../components/ApiAuthImage';
import { fileNameFromUrl } from '../../api/files';
import { useAuthStore } from '../../stores/authStore';
import type {
  TrainInfoClassListItem,
  TrainInfoClassCreateReq,
  TrainInfoListCreateReq,
  TrainInfoListUpdateReq,
  TrainInfoList,
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
import { ClassSignUpPeoplePanel } from './ClassSignUpPeoplePanel';
import { ClassRolesModal } from './ClassRolesPanel';

const { Dragger } = Upload;

/** 与需求管理等页一致的工种等级选项 */
const WORK_LEVELS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

function workLevelLabel(level?: number | string | null): string {
  const n = Number(level);
  if (!Number.isFinite(n) || n <= 0) return '';
  return WORK_LEVELS.find((o) => o.value === n)?.label ?? `${n}级`;
}

function toPositiveInt(value: unknown): number | undefined {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? n : undefined;
}

/** 兼容 json-bigint：超大 ID 保持字符串，避免 Number 精度丢失 */
type ApiId = number | string;

function toApiId(value: unknown): ApiId | null {
  if (value == null || value === '') return null;
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }
  const s = String(value).trim();
  if (!s) return null;
  if (/^\d+$/.test(s)) {
    const n = Number(s);
    return Number.isSafeInteger(n) ? n : s;
  }
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

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
    id: toApiId(pick('id')) ?? 0,
    realname: String(pick('realname', 'name') ?? ''),
    phone: pick('phone') != null ? String(pick('phone')) : undefined,
    train_id: toApiId(pick('train_id', 'trainId')) ?? undefined,
    train_class_id: toApiId(pick('train_class_id', 'trainClassId')) ?? undefined,
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
  workTypeTitle,
  workTypeLevel,
  workTypeId,
  mode = 'roster',
  onProfileChanged,
}: {
  open: boolean;
  onClose: () => void;
  classId: ApiId | null;
  trainInfoId: ApiId | null;
  workTypeTitle?: string;
  workTypeLevel?: number;
  workTypeId?: number;
  mode?: 'roster' | 'collection';
  onProfileChanged?: () => void;
}) {
  const [collectionSignUpId, setCollectionSignUpId] = useState<ApiId | null>(null);
  const [collectionTitle, setCollectionTitle] = useState<string | undefined>();
  /** idle→loading→ready|error，避免打开瞬间误显示「未就绪」 */
  const [collectionStatus, setCollectionStatus] = useState<'idle' | 'loading' | 'ready' | 'error'>('idle');
  const [collectionError, setCollectionError] = useState<string | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [importOpen, setImportOpen] = useState(false);
  const [importByIdCardOpen, setImportByIdCardOpen] = useState(false);
  const [importKeyword, setImportKeyword] = useState('');
  const [selectedSignUpIds, setSelectedSignUpIds] = useState<Array<number | string>>([]);
  const [selectedStudentIds, setSelectedStudentIds] = useState<Array<number | string>>([]);
  const [idCardText, setIdCardText] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 20;

  const normalizedClassId = classId != null ? toApiId(classId) : null;

  const ensureCollectionMu = useMutation({
    mutationFn: (id: ApiId) => trainApi.ensureClassCollection(id),
    onSuccess: (res) => {
      const sid = toApiId(res.signUpId);
      if (sid == null) {
        setCollectionStatus('error');
        setCollectionError('后端未返回资料收集计划 ID');
        message.error('打开资料收集失败：未返回计划 ID');
        return;
      }
      setCollectionSignUpId(sid);
      setCollectionTitle(res.title);
      setCollectionStatus('ready');
      setCollectionError(null);
      onProfileChanged?.();
    },
    onError: (err: unknown) => {
      const ax = err as { response?: { data?: { message?: string } }; message?: string };
      const msg = ax.response?.data?.message || ax.message || '打开资料收集失败';
      setCollectionStatus('error');
      setCollectionError(msg);
      message.error(msg);
    },
  });

  useEffect(() => {
    if (!open) {
      setCurrentPage(1);
      setCollectionSignUpId(null);
      setCollectionTitle(undefined);
      setCollectionStatus('idle');
      setCollectionError(null);
      setSelectedStudentIds([]);
      return;
    }
    setCurrentPage(1);
    if (mode === 'collection' && normalizedClassId != null) {
      setCollectionSignUpId(null);
      setCollectionTitle(undefined);
      setCollectionError(null);
      setCollectionStatus('loading');
      ensureCollectionMu.mutate(normalizedClassId);
    } else {
      setCollectionSignUpId(null);
      setCollectionTitle(undefined);
      setCollectionStatus('idle');
      setCollectionError(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, classId, mode]);

  const { data, isLoading, refetch, isError } = useQuery({
    queryKey: ['class-students', normalizedClassId, currentPage, pageSize],
    queryFn: () => trainApi.listClassStudents(normalizedClassId!, currentPage, pageSize),
    enabled: open && mode === 'roster' && normalizedClassId != null,
  });

  const { data: signUpData } = useQuery({
    queryKey: ['sign-up-self', trainInfoId, normalizedClassId, importKeyword],
    queryFn: () => trainApi.listSignUpSelf(trainInfoId!, importKeyword || undefined, normalizedClassId ?? undefined),
    enabled: importOpen && !!trainInfoId,
  });

  const saveMu = useMutation({
    mutationFn: (p: Record<string, unknown>) => trainApi.saveClassStudent(p),
    onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (ids: Array<number | string>) => trainApi.deleteClassStudents(ids),
    onSuccess: (_res, ids) => {
      message.success(ids.length > 1 ? `已删除 ${ids.length} 人` : '删除成功');
      setSelectedStudentIds((prev) => prev.filter((id) => !ids.map(String).includes(String(id))));
      refetch();
    },
    onError: () => message.error('删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: ApiId; classId: ApiId; studentIds: Array<number | string> }) => trainApi.importStudents(p),
    onSuccess: (res) => {
      const sync = res?.platformUserSync;
      const created = sync?.created ?? 0;
      const skipped = sync?.skipped ?? 0;
      if (created > 0) {
        message.success(`导入成功，新建学员账号 ${created} 人（默认密码 123123）`);
      } else if (skipped > 0) {
        message.warning(`导入完成，但有 ${skipped} 人未能开通学员账号，请查看失败原因`);
      } else {
        message.success('导入成功');
      }
      setImportOpen(false);
      setSelectedSignUpIds([]);
      refetch();
    },
    onError: () => message.error('导入失败'),
  });

  const importByIdCardMu = useMutation({
    mutationFn: (p: { traininfoId: ApiId; classId: ApiId; studentIds: string }) => trainApi.importStudentsByIdCard(p),
    onSuccess: (res) => {
      const sync = res?.platformUserSync;
      const created = sync?.created ?? 0;
      const skipped = sync?.skipped ?? 0;
      if (created > 0) {
        message.success(`导入成功，新建学员账号 ${created} 人（默认密码 123123）`);
      } else if (skipped > 0) {
        message.warning(`导入完成，但有 ${skipped} 人未能开通学员账号，请查看失败原因`);
      } else {
        message.success('导入成功');
      }
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
        {/* 表格缩略图用原生 img：失败时隐藏，避免 ant-image-error 裂图图标 */}
        <AuthImage
          url={url}
          alt="证件照"
          className="student-id-photo"
          width={72}
          height={96}
          style={{ objectFit: 'cover', borderRadius: 2 }}
        />
      </div>
    );
  };

  const handleDeleteStudent = (row: ClassStudent) => {
    Modal.confirm({
      title: '友情提示',
      content: '确定要删除该学员吗？',
      onOk: () => deleteMu.mutateAsync([row.id]),
    });
  };

  const handleBatchDeleteStudents = () => {
    if (!selectedStudentIds.length) {
      message.warning('请先勾选要删除的学员');
      return;
    }
    Modal.confirm({
      title: '友情提示',
      content: `确定删除已选的 ${selectedStudentIds.length} 名学员吗？不会删除学员 App 账号。`,
      okButtonProps: { danger: true },
      onOk: () => deleteMu.mutateAsync(selectedStudentIds),
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
      message.error('班级未关联培训信息，无法从报名导入');
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
      message.error('班级未关联培训信息，无法按身份证导入');
      return;
    }
    importByIdCardMu.mutate({ traininfoId: trainInfoId, classId: normalizedClassId, studentIds: idCardText });
  };

  return (
    <>
      <Modal
        title={mode === 'collection' ? '资料收集' : '班级人员'}
        open={open}
        onCancel={onClose}
        footer={null}
        width="88%"
        centered
        destroyOnHidden
        rootClassName="class-student-list-modal"
        styles={{ body: { padding: '12px 16px 16px', maxHeight: '78vh', overflow: 'hidden' } }}
      >
        {mode === 'collection' ? (
          collectionStatus === 'loading' || (collectionStatus === 'idle' && !collectionSignUpId) ? (
            <div style={{ padding: 24, textAlign: 'center' }}>正在准备资料收集…</div>
          ) : collectionStatus === 'ready' && collectionSignUpId != null ? (
            <div className="class-student-table-scroll" style={{ maxHeight: '68vh', overflow: 'auto' }}>
              <ClassSignUpPeoplePanel
                signUpId={collectionSignUpId}
                title={collectionTitle}
                classWorkTypeTitle={workTypeTitle}
                classWorkTypeLevel={workTypeLevel}
                classWorkTypeId={workTypeId}
                onChanged={() => {
                  onProfileChanged?.();
                }}
              />
            </div>
          ) : (
            <div style={{ padding: 24, textAlign: 'center' }}>
              <div style={{ marginBottom: 12 }}>
                {collectionError || '资料收集计划未就绪'}
              </div>
              {normalizedClassId != null ? (
                <Button
                  type="primary"
                  loading={ensureCollectionMu.isPending}
                  onClick={() => {
                    setCollectionError(null);
                    setCollectionStatus('loading');
                    ensureCollectionMu.mutate(normalizedClassId);
                  }}
                >
                  重试
                </Button>
              ) : null}
            </div>
          )
        ) : (
          <>
            <div className="student-toolbar">
              <Button
                className="student-toolbar-btn"
                disabled={!trainInfoId}
                title={!trainInfoId ? '班级未关联培训信息' : undefined}
                onClick={() => setImportOpen(true)}
              >
                批量导入
              </Button>
              <Button
                className="student-toolbar-btn"
                disabled={!trainInfoId}
                title={!trainInfoId ? '班级未关联培训信息' : undefined}
                onClick={() => setImportByIdCardOpen(true)}
              >
                按身份证号导入
              </Button>
              <Button
                className="student-toolbar-btn"
                danger
                disabled={!selectedStudentIds.length}
                loading={deleteMu.isPending}
                onClick={handleBatchDeleteStudents}
              >
                批量删除
              </Button>
              {isError ? (
                <Button className="student-toolbar-btn" onClick={() => refetch()}>
                  重新加载
                </Button>
              ) : null}
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
                rowSelection={{
                  columnWidth: 48,
                  selectedRowKeys: selectedStudentIds,
                  onChange: (keys) => setSelectedStudentIds(keys as Array<number | string>),
                }}
                scroll={{ x: 1100, y: 480 }}
                pagination={false}
                locale={{ emptyText: isError ? '加载失败，请点击重新加载' : '暂无学员，可从报名或身份证导入' }}
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
          </>
        )}
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
          rowSelection={{ columnWidth: 55, selectedRowKeys: selectedSignUpIds, onChange: (keys) => setSelectedSignUpIds(keys as Array<number | string>) }}
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
  classId: ApiId | null;
  trainInfoId: ApiId | null;
  tag: string;
  title: string;
}) {
  const [editOpen, setEditOpen] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editForm] = Form.useForm();
  const [importOpen, setImportOpen] = useState(false);
  const [selectedTeacherIds, setSelectedTeacherIds] = useState<Array<number | string>>([]);

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
    mutationFn: (ids: Array<number | string>) => trainApi.deleteClassOthers(ids),
    onSuccess: () => { message.success('删除成功'); refetch(); },
    onError: () => message.error('删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (p: { traininfoId: ApiId; classId: ApiId; tag: string; ids: Array<number | string> }) => trainApi.importOthers(p),
    onSuccess: () => { message.success('导入成功'); setImportOpen(false); setSelectedTeacherIds([]); refetch(); },
    onError: () => message.error('导入失败'),
  });

  const openAdd = () => {
    editForm.resetFields();
    // tag=fuzhe → type=inCharge；教务走教师库导入，不会进入此分支
    editForm.setFieldsValue({
      id: 0,
      train_id: trainInfoId,
      classid: classId,
      type: tag === 'teacher' ? 'staff' : 'inCharge',
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
            onChange: (keys) => setSelectedTeacherIds(keys as Array<number | string>),
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
  classId: ApiId | null;
  kejianOptions: { value: number; label: string }[];
}) {
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [editingId, setEditingId] = useState<ApiId | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['class-tasks', classId],
    queryFn: () => trainApi.listClassTasks(classId!),
    enabled: open && !!classId,
  });

  const saveMu = useMutation({
    mutationFn: (p: { id?: ApiId; classId?: ApiId; ck_id?: number; date?: string }) => trainApi.saveClassTask(p),
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
      title: '操作', key: 'act', width: 100, fixed: 'right', align: 'center',
      render: (_, r) => (
        <Button type="link" size="small" className="link-action" onClick={() => {
          setEditingId(r.id);
          editForm.setFieldsValue({ ck_id: r.ck_id, date: r.date ? dayjs(r.date) : undefined });
          setEditOpen(true);
        }}>编辑</Button>
      ),
    },
  ];

  return (
    <>
      <Modal title="培训任务" open={open} onCancel={onClose} footer={null} width="60%" destroyOnHidden>
        <div className="mar-b10">
          <Button onClick={() => {
            setEditingId(null);
            editForm.resetFields();
            setEditOpen(true);
          }}>新增</Button>
        </div>
        <Table rowKey="id" bordered loading={isLoading} columns={columns}
          dataSource={data?.list ?? []} scroll={{ y: 400 }} pagination={false}
          locale={{ emptyText: '暂无培训任务' }} />
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
  const canCreateTrainInfo = useAuthStore((s) => s.permissionCodes.includes('train:info-list:create'));
  const canUpdateTrainInfo = useAuthStore((s) => s.permissionCodes.includes('train:info-list:update'));
  const [form] = Form.useForm<{
    id?: ApiId;
    title?: string;
    trainInfoId?: ApiId;
    workTypeId?: number;
    workTypeLevel?: number;
    nums?: number;
    dateStart?: Dayjs;
    dateEnd?: Dayjs;
    kcbiaoFile?: string;
    kcbiaoFileDisplay?: string;
    companyContactName?: string;
    companyContactPhone?: string;
  }>();
  const [trainInfoForm] = Form.useForm<{
    title?: string;
    companyName?: string;
    num?: number;
    startDate?: Dayjs;
    endDate?: Dayjs;
  }>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [isCreateMode, setIsCreateMode] = useState(true);
  const [trainInfoModalOpen, setTrainInfoModalOpen] = useState(false);
  const [editingTrainInfoId, setEditingTrainInfoId] = useState<ApiId | null>(null);
  const [trainInfoDetailLoading, setTrainInfoDetailLoading] = useState(false);
  const selectedTrainInfoId = Form.useWatch('trainInfoId', form);
  /** 本页刚创建的培训，避免 publicData 刷新前 Select 无选项 */
  const [extraTrainInfoOptions, setExtraTrainInfoOptions] = useState<{ value: ApiId; label: string }[]>([]);
  /** 手填企业名，并入下拉（含本页刚录入、以及历史培训信息里的自由文本企业） */
  const [extraCompanyNames, setExtraCompanyNames] = useState<string[]>([]);
  const [companySearch, setCompanySearch] = useState('');
  const [kcbiaoSrc, setKcbiaoSrc] = useState('');

  const [uploadOpen, setUploadOpen] = useState(false);
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);

  const [studentModal, setStudentModal] = useState(false);
  const [studentModalMode, setStudentModalMode] = useState<'roster' | 'collection'>('roster');
  const [staffModal, setStaffModal] = useState(false);
  const [chargeModal, setChargeModal] = useState(false);
  const [rolesModal, setRolesModal] = useState(false);
  const [taskModal, setTaskModal] = useState(false);
  const [selectedClass, setSelectedClass] = useState<{
    id: ApiId;
    trainInfoId: ApiId;
    title?: string;
    workTypeTitle?: string;
    workTypeLevel?: number;
    workTypeId?: number;
  } | null>(null);

  const { data: publicData } = useQuery({
    queryKey: ['class-people-public-data'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'worktype', 'traininfo', 'kejian']),
    staleTime: 5 * 60 * 1000,
  });

  const { data: listData, isLoading, isError, refetch } = useQuery({
    queryKey: ['info-classes', currentPage, pageSize, companyIdFilter, workTypeIdFilter, keyword],
    queryFn: () => trainApi.pageInfoClassList({
      page: currentPage,
      pageSize,
      companyId: companyIdFilter,
      workTypeId: workTypeIdFilter,
      keyword: keyword || undefined,
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
    mutationFn: (id: ApiId) => trainApi.batchDeleteInfoClasses([id]),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['info-classes'] });
    },
    onError: () => message.error('删除失败'),
  });

  const createTrainInfoMu = useMutation({
    mutationFn: (payload: TrainInfoListCreateReq) =>
      unwrap<TrainInfoList>(
        apiClient.post('/training/info-lists', payload, { silentError: true }),
      ),
    onSuccess: async (created) => {
      message.success('培训信息已创建');
      const createdId = String(created.id);
      const createdTitle =
        String(created.title ?? '').trim()
        || String(trainInfoForm.getFieldValue('title') ?? '').trim()
        || `培训#${createdId}`;
      setExtraTrainInfoOptions((prev) => [
        { value: createdId, label: createdTitle },
        ...prev.filter((o) => String(o.value) !== createdId),
      ]);
      await queryClient.invalidateQueries({ queryKey: ['class-people-public-data'] });
      await queryClient.invalidateQueries({ queryKey: ['exam-score-import-public'] });
      const patch: {
        trainInfoId?: ApiId;
        nums?: number;
        dateStart?: Dayjs;
        dateEnd?: Dayjs;
      } = { trainInfoId: createdId };
      const classVals = form.getFieldsValue();
      if (classVals.nums == null && created.num != null) {
        patch.nums = created.num;
      }
      if (!classVals.dateStart && created.startDate) {
        patch.dateStart = dayjs(created.startDate);
      }
      if (!classVals.dateEnd && created.endDate) {
        patch.dateEnd = dayjs(created.endDate);
      }
      form.setFieldsValue(patch);
      setTrainInfoModalOpen(false);
      trainInfoForm.resetFields();
      setEditingTrainInfoId(null);
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const dataMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      const msg = dataMsg || (err instanceof Error ? err.message : '');
      if (status === 403 || /权限|forbidden|access denied/i.test(msg)) {
        message.error('无创建培训信息权限（需要 train:info-list:create）');
        return;
      }
      message.error(msg || '创建培训信息失败');
    },
  });

  const updateTrainInfoMu = useMutation({
    mutationFn: ({ id, payload }: { id: ApiId; payload: TrainInfoListUpdateReq }) =>
      trainApi.updateInfoList(id, payload),
    onSuccess: async (updated, vars) => {
      message.success('培训信息已保存');
      const updatedId = String(updated.id ?? vars.id);
      const label =
        String(updated.title ?? '').trim()
        || String(trainInfoForm.getFieldValue('title') ?? '').trim()
        || `培训#${updatedId}`;
      setExtraTrainInfoOptions((prev) => {
        const next = prev.filter(
          (o) => String(o.value) !== String(vars.id) && String(o.value) !== updatedId,
        );
        return [{ value: updatedId, label }, ...next];
      });
      if (String(form.getFieldValue('trainInfoId') ?? '') === String(vars.id)) {
        form.setFieldValue('trainInfoId', updatedId);
      }
      await queryClient.invalidateQueries({ queryKey: ['class-people-public-data'] });
      await queryClient.invalidateQueries({ queryKey: ['exam-score-import-public'] });
      await queryClient.invalidateQueries({ queryKey: ['info-classes'] });
      setTrainInfoModalOpen(false);
      trainInfoForm.resetFields();
      setEditingTrainInfoId(null);
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const dataMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      const msg = dataMsg || (err instanceof Error ? err.message : '');
      if (status === 403 || /权限|forbidden|access denied/i.test(msg)) {
        message.error('无编辑培训信息权限（需要 train:info-list:update）');
        return;
      }
      message.error(msg || '保存培训信息失败');
    },
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

  const companyNameOptions = useMemo(() => {
    const seen = new Set<string>();
    const names: string[] = [];
    const add = (raw?: string | null) => {
      const n = String(raw ?? '').trim();
      if (!n) return;
      const key = n.toLowerCase();
      if (seen.has(key)) return;
      seen.add(key);
      names.push(n);
    };
    for (const c of companyOptions) add(c.label);
    for (const t of (publicData as DemandPublicData | undefined)?.traininfo_list ?? []) {
      add(t.company_name);
    }
    for (const n of extraCompanyNames) add(n);
    add(companySearch);
    return names.map((value) => ({ value, label: value }));
  }, [companyOptions, publicData, extraCompanyNames, companySearch]);

  const trainInfoOptions = useMemo(() => {
    const map = new Map<string, { value: string; label: string }>();
    for (const t of (publicData as DemandPublicData | undefined)?.traininfo_list ?? []) {
      const id = String(t.id);
      const title = String(t.title ?? '').trim();
      map.set(id, { value: id, label: title || `培训#${id}` });
    }
    for (const o of extraTrainInfoOptions) {
      const id = String(o.value);
      const label = String(o.label ?? '').trim();
      const existing = map.get(id);
      // 本页刚建的标题优先，避免 publicData 刷新前/空标题时 Select 只显示 id
      if (!existing || !existing.label || existing.label.startsWith('培训#')) {
        map.set(id, { value: id, label: label || existing?.label || `培训#${id}` });
      } else if (label && !label.startsWith('培训#')) {
        map.set(id, { value: id, label });
      }
    }
    return Array.from(map.values());
  }, [publicData, extraTrainInfoOptions]);

  const kejianOptions = useMemo(() => {
    return (publicData as DemandPublicData | undefined)?.kejian_list?.map((k) => ({
      value: Number(k.id),
      label: k.title,
    })) ?? [];
  }, [publicData]);

  const closeModal = () => {
    setModalOpen(false);
    setTrainInfoModalOpen(false);
    setEditingTrainInfoId(null);
    setKcbiaoSrc('');
    form.resetFields();
    trainInfoForm.resetFields();
  };

  const openCreate = () => {
    form.resetFields();
    setKcbiaoSrc('');
    setIsCreateMode(true);
    setModalOpen(true);
  };

  const openEdit = (row: TrainInfoClassListItem) => {
    setIsCreateMode(false);
    form.setFieldsValue({
      id: row.id,
      title: row.title,
      trainInfoId: row.train_info_id != null ? String(row.train_info_id) : undefined,
      workTypeId: toPositiveInt(row.work_type_id),
      workTypeLevel: toPositiveInt(row.work_type_level),
      nums: row.nums,
      dateStart: row.date_start ? dayjs(row.date_start) : undefined,
      dateEnd: row.date_end ? dayjs(row.date_end) : undefined,
      kcbiaoFile: row.kcbiao_file,
      kcbiaoFileDisplay: row.kcbiao_file,
      companyContactName: row.company_contact_name,
      companyContactPhone: row.company_contact_phone,
    });
    setKcbiaoSrc(row.kcbiao_src ?? resolveFileUrl(row.kcbiao_file));
    setModalOpen(true);
  };

  const openCreateTrainInfo = () => {
    if (!canCreateTrainInfo) {
      message.error('无创建培训信息权限（需要 train:info-list:create）');
      return;
    }
    setEditingTrainInfoId(null);
    trainInfoForm.resetFields();
    setCompanySearch('');
    setTrainInfoModalOpen(true);
  };

  const openEditTrainInfo = async () => {
    const id = toApiId(form.getFieldValue('trainInfoId'));
    if (id == null) {
      message.warning('请先选择培训信息');
      return;
    }
    if (!canUpdateTrainInfo) {
      message.error('无编辑培训信息权限（需要 train:info-list:update）');
      return;
    }
    setTrainInfoDetailLoading(true);
    try {
      const detail = await trainApi.getInfoList(id);
      const raw = detail as TrainInfoList & {
        company_name?: string;
      };
      setEditingTrainInfoId(id);
      setCompanySearch('');
      trainInfoForm.setFieldsValue({
        title: detail.title,
        companyName: detail.companyName ?? raw.company_name,
      });
      const companyName = String(detail.companyName ?? raw.company_name ?? '').trim();
      if (
        companyName
        && !companyOptions.some((c) => String(c.label).trim() === companyName)
      ) {
        setExtraCompanyNames((prev) =>
          prev.some((n) => n.toLowerCase() === companyName.toLowerCase())
            ? prev
            : [...prev, companyName],
        );
      }
      setTrainInfoModalOpen(true);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const dataMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      const msg = dataMsg || (err instanceof Error ? err.message : '');
      if (status === 403 || /权限|forbidden|access denied/i.test(msg)) {
        message.error('无查看培训信息权限（需要 train:info-list:view）');
        return;
      }
      message.error(msg || '加载培训信息失败');
    } finally {
      setTrainInfoDetailLoading(false);
    }
  };

  const handleSubmitTrainInfo = async () => {
    try {
      const v = await trainInfoForm.validateFields();
      if (v.startDate && v.endDate && dayjs(v.endDate).isBefore(dayjs(v.startDate), 'day')) {
        message.error('结束日期不能早于开始日期');
        return;
      }
      const companyName = String(v.companyName ?? '').trim();
      if (!companyName) {
        message.error('请选择或输入企业');
        return;
      }
      const matchedCompany = companyOptions.find(
        (c) => String(c.label).trim() === companyName,
      );
      if (!matchedCompany) {
        setExtraCompanyNames((prev) =>
          prev.some((n) => n.toLowerCase() === companyName.toLowerCase())
            ? prev
            : [...prev, companyName],
        );
      }
      const payload: TrainInfoListCreateReq = {
        title: v.title,
        companyId: matchedCompany?.value as number | undefined,
        companyName,
        num: v.num,
        startDate: v.startDate ? dayjs(v.startDate).format('YYYY-MM-DD') : undefined,
        endDate: v.endDate ? dayjs(v.endDate).format('YYYY-MM-DD') : undefined,
      };
      if (editingTrainInfoId != null) {
        updateTrainInfoMu.mutate({ id: editingTrainInfoId, payload });
        return;
      }
      createTrainInfoMu.mutate({ ...payload, status: 0 });
    } catch {
      // Form validation errors are shown inline
    }
  };

  const handleSubmit = async () => {
    try {
      const v = await form.validateFields();
      if (v.dateStart && v.dateEnd && dayjs(v.dateEnd).isBefore(dayjs(v.dateStart), 'day')) {
        message.error('结束日期不能早于开始日期');
        return;
      }
      const payload: TrainInfoClassCreateReq = {
        id: v.id,
        title: v.title,
        trainInfoId: v.trainInfoId,
        workTypeId: toPositiveInt(v.workTypeId),
        workTypeLevel: toPositiveInt(v.workTypeLevel),
        nums: Number(v.nums),
        dateStart: dayjs.isDayjs(v.dateStart) ? v.dateStart.format('YYYY-MM-DD') : String(v.dateStart),
        dateEnd: dayjs.isDayjs(v.dateEnd) ? v.dateEnd.format('YYYY-MM-DD') : String(v.dateEnd),
        kcbiaoFile: v.kcbiaoFile,
        companyContactName: String(v.companyContactName ?? '').trim(),
        companyContactPhone: String(v.companyContactPhone ?? '').trim(),
      };
      saveMu.mutate(payload);
    } catch {
      // Form validation errors are shown inline
    }
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
    setKeywordInput('');
    setKeyword('');
    setCompanyIdFilter(undefined);
    setWorkTypeIdFilter(undefined);
    setCurrentPage(1);
  };

  const applyKeywordSearch = () => {
    setKeyword(keywordInput.trim());
    setCurrentPage(1);
  };

  const showCreateTrainInfoBtn = canCreateTrainInfo && isCreateMode;
  const showEditTrainInfoBtn = canUpdateTrainInfo && selectedTrainInfoId != null;
  const trainInfoSelectExtraPx = (showCreateTrainInfoBtn ? 88 : 0) + (showEditTrainInfoBtn ? 72 : 0);

  const openSubDialog = (type: string, row: TrainInfoClassListItem) => {
    const id = toApiId(row.id);
    const trainInfoId = toApiId(row.train_info_id) ?? 0;
    if (id == null) {
      message.error('班级 ID 无效');
      return;
    }
    setSelectedClass({
      id,
      trainInfoId,
      title: row.title,
      workTypeTitle: row.work_type_title,
      workTypeLevel: toPositiveInt(row.work_type_level),
      workTypeId: toPositiveInt(row.work_type_id),
    });
    switch (type) {
      case 'students':
        setStudentModalMode('roster');
        setStudentModal(true);
        break;
      case 'collection':
        setStudentModalMode('collection');
        setStudentModal(true);
        break;
      case 'staff': setStaffModal(true); break;
      case 'charge': setChargeModal(true); break;
      case 'roles': setRolesModal(true); break;
      case 'tasks': setTaskModal(true); break;
    }
  };

  const closeStudentModal = () => {
    setStudentModal(false);
  };

  const formatCreateTime = (row: TrainInfoClassListItem) => {
    if (row.addtime_cn) return row.addtime_cn;
    if (row.addtime) return dayjs.unix(row.addtime).format('YYYY-MM-DD HH:mm:ss');
    return '-';
  };

  const formatDateRange = (row: TrainInfoClassListItem) => {
    if (!row.date_start && !row.date_end) return '-';
    return `${row.date_start ?? '—'} ~ ${row.date_end ?? '—'}`;
  };

  const handleDeleteClass = (row: TrainInfoClassListItem) => {
    Modal.confirm({
      title: '友情提示',
      content: (
        <div>
          <p>确定删除班级「{row.title}」吗？将同时：</p>
          <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
            <li>删除本班花名册、培训任务、资料收集记录</li>
            <li>把对应名单人员回滚为「待分班」，便于重新分班</li>
            <li>解绑排课记录中的班级关联</li>
          </ul>
          <p style={{ marginTop: 8, color: '#666' }}>不会删除学员 App 账号（学员信息仍保留）。</p>
        </div>
      ),
      onOk: () => deleteMu.mutate(row.id),
    });
  };

  const renderRowActions = (row: TrainInfoClassListItem) => (
    <div className="class-row-actions">
      <div className="class-row-actions-line">
        <button type="button" className="class-action-link" onClick={() => openSubDialog('students', row)}>学员列表</button>
        <button type="button" className="class-action-link" onClick={() => openSubDialog('collection', row)}>资料收集</button>
        {/* <button type="button" className="class-action-link" onClick={() => openSubDialog('staff', row)}>教务人员</button> */}
        <button type="button" className="class-action-link" onClick={() => openSubDialog('roles', row)}>班级角色</button>
      </div>
      <div className="class-row-actions-line">
        <button type="button" className="class-action-link" onClick={() => openSubDialog('charge', row)}>负责人</button>
        {/* <button type="button" className="class-action-link" onClick={() => openSubDialog('tasks', row)}>培训任务</button> */}
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
    {
      title: '班级名称',
      dataIndex: 'title',
      align: 'left',
      width: 280,
      className: 'class-wrap-cell',
      onCell: () => ({ className: 'class-wrap-cell' }),
    },
    {
      title: '公司',
      dataIndex: 'company_name',
      align: 'left',
      width: 180,
      className: 'class-wrap-cell',
      onCell: () => ({ className: 'class-wrap-cell' }),
    },
    { title: '工种', dataIndex: 'work_type_title', align: 'center', width: 140 },
    {
      title: '等级',
      dataIndex: 'work_type_level',
      align: 'center',
      width: 130,
      render: (v?: number) => workLevelLabel(v) || '-',
    },
    { title: '人数', dataIndex: 'nums', width: 72, align: 'center' },
    {
      title: '培训周期',
      key: 'dateRange',
      width: 200,
      align: 'center',
      render: (_, row) => formatDateRange(row),
    },
    {
      title: '资料进度',
      key: 'profile',
      width: 120,
      align: 'center',
      render: (_, row) => {
        const approved = Number(row.profile_approved ?? 0);
        const total = Number(row.profile_total ?? 0);
        if (!row.collection_sign_up_id && total === 0) {
          return (
            <button type="button" className="class-action-link" onClick={() => openSubDialog('collection', row)}>
              去收集
            </button>
          );
        }
        return (
          <button
            type="button"
            className="class-action-link"
            style={{ color: row.profile_ready ? '#389e0d' : undefined }}
            onClick={() => openSubDialog('collection', row)}
            title="打开资料收集"
          >
            {approved}/{total}{row.profile_ready ? ' ✓' : ''}
          </button>
        );
      },
    },
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
      width: 280,
      align: 'center',
      fixed: 'right',
      render: (_, row) => renderRowActions(row),
    },
  ];

  return (
    <div className="training-class-people-page training-page">
      <section className="page-heading">
        <Typography.Title level={4}>开班配置</Typography.Title>
        <Typography.Text type="secondary">
          维护已分班班级的学员花名册、教务负责人、培训任务与资料收集；试题配置与刷题学情请到「跟班」工作台
        </Typography.Text>
      </section>

      <TrainingProcessSteps current={processStep} />

      <Card size="small" style={{ marginBottom: 16 }}>
      <div className="search-filter-bar">
        <div className="filter-actions">
          <Input.Search
            className="filter-keyword"
            allowClear
            placeholder="搜索班级名称"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            onSearch={applyKeywordSearch}
            enterButton="查询"
          />
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
          <Button onClick={handleReset}>重置</Button>
          <Button type="primary" onClick={openCreate}>创建班级</Button>
          {isError ? <Button onClick={() => refetch()}>重新加载</Button> : null}
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
        scroll={{ x: 1280, y: 600 }}
        pagination={false}
        locale={{ emptyText: isError ? '加载失败，请点击重新加载' : '暂无班级，可点击「创建班级」或先完成上游分班' }}
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
          <Form.Item name="title" label="班级名称" rules={[{ required: true, message: '请填写班级名称' }]}>
            <Input placeholder="请输入班级名称" />
          </Form.Item>
          <Form.Item label="选择培训信息" required>
            <Space.Compact style={{ width: '100%' }}>
              <Form.Item
                name="trainInfoId"
                noStyle
                rules={[{ required: true, message: '请选择培训信息' }]}
              >
                <Select
                  placeholder="请选择培训信息"
                  options={trainInfoOptions}
                  showSearch
                  optionFilterProp="label"
                  optionLabelProp="label"
                  style={{
                    width: trainInfoSelectExtraPx > 0 ? `calc(100% - ${trainInfoSelectExtraPx}px)` : '100%',
                  }}
                  onChange={(id) => {
                    if (form.getFieldValue('workTypeId')) return;
                    const matched = (publicData as DemandPublicData | undefined)?.traininfo_list
                      ?.find((t) => String(t.id) === String(id));
                    if (matched?.work_type_id) {
                      form.setFieldValue('workTypeId', matched.work_type_id);
                    }
                  }}
                />
              </Form.Item>
              {showCreateTrainInfoBtn ? (
                <Button icon={<PlusOutlined />} onClick={openCreateTrainInfo}>
                  新建
                </Button>
              ) : null}
              {showEditTrainInfoBtn ? (
                <Button icon={<EditOutlined />} onClick={openEditTrainInfo} loading={trainInfoDetailLoading}>
                  编辑
                </Button>
              ) : null}
            </Space.Compact>
          </Form.Item>
          <Form.Item name="workTypeId" label="预约工种" rules={[{ required: true, message: '请选择工种' }]}>
            <Select
              placeholder="请选择本班工种"
              options={workTypeGroupedOptions}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="workTypeLevel" label="预约工种等级" rules={[{ required: true, message: '请选择工种等级' }]}>
            <Select placeholder="请选择本班等级" options={WORK_LEVELS} />
          </Form.Item>
          <Form.Item name="nums" label="班级人数" rules={[{ required: true, message: '请填写班级人数' }]}>
            <Input type="number" placeholder="请输入班级人数" min={1} />
          </Form.Item>
          <Form.Item name="companyContactName" label="单位联系人">
            <Input placeholder="请输入单位联系人（用于工作年限证明）" maxLength={64} />
          </Form.Item>
          <Form.Item name="companyContactPhone" label="联系方式">
            <Input placeholder="请输入联系方式（用于工作年限证明）" maxLength={32} />
          </Form.Item>
          <Form.Item name="dateStart" label="开始日期" rules={[{ required: true, message: '请选择开始日期' }]}>
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" placeholder="选择开始日期" />
          </Form.Item>
          <Form.Item name="dateEnd" label="结束日期" rules={[{ required: true, message: '请选择结束日期' }]}>
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
        title={editingTrainInfoId != null ? '编辑培训信息' : '新建培训信息'}
        open={trainInfoModalOpen}
        onOk={handleSubmitTrainInfo}
        onCancel={() => {
          setTrainInfoModalOpen(false);
          setEditingTrainInfoId(null);
          trainInfoForm.resetFields();
        }}
        confirmLoading={createTrainInfoMu.isPending || updateTrainInfoMu.isPending}
        okText={editingTrainInfoId != null ? '保存' : '创建并选用'}
        cancelText="取消"
        destroyOnHidden
        width={520}
      >
        <Typography.Paragraph type="secondary" style={{ marginTop: 0 }}>
          {editingTrainInfoId != null
            ? '修改标题、企业后保存。工种和等级请在班级信息里按班填写，互不影响。'
            : '创建后自动选入上方「选择培训信息」。同一企业可挂多个班级，各班工种和等级分别设置。'}
        </Typography.Paragraph>
        <Form form={trainInfoForm} labelCol={{ style: { width: 100 } }} wrapperCol={{ span: 16 }}>
          <Form.Item name="title" label="培训信息" rules={[{ required: true, message: '请填写培训信息' }]}>
            <Input placeholder="请输入培训信息标题" maxLength={255} />
          </Form.Item>
          <Form.Item
            name="companyName"
            label="企业名称"
            rules={[{ required: true, message: '请选择或输入企业' }]}
          >
            <Select
              allowClear
              showSearch
              style={{ width: '100%' }}
              placeholder="请选择或输入企业"
              options={companyNameOptions}
              searchValue={companySearch}
              onSearch={setCompanySearch}
              filterOption={(input, option) =>
                String(option?.label ?? '')
                  .toLowerCase()
                  .includes(input.trim().toLowerCase())
              }
              onChange={(v) => {
                setCompanySearch('');
                const name = String(v ?? '').trim();
                if (
                  name
                  && !companyOptions.some((c) => String(c.label).trim() === name)
                ) {
                  setExtraCompanyNames((prev) =>
                    prev.some((n) => n.toLowerCase() === name.toLowerCase())
                      ? prev
                      : [...prev, name],
                  );
                }
              }}
              onBlur={() => {
                const typed = companySearch.trim();
                if (!typed) return;
                trainInfoForm.setFieldValue('companyName', typed);
                if (!companyOptions.some((c) => String(c.label).trim() === typed)) {
                  setExtraCompanyNames((prev) =>
                    prev.some((n) => n.toLowerCase() === typed.toLowerCase())
                      ? prev
                      : [...prev, typed],
                  );
                }
                setCompanySearch('');
              }}
              onInputKeyDown={(e) => {
                if (e.key !== 'Enter') return;
                const typed = companySearch.trim();
                if (!typed) return;
                e.preventDefault();
                trainInfoForm.setFieldValue('companyName', typed);
                if (!companyOptions.some((c) => String(c.label).trim() === typed)) {
                  setExtraCompanyNames((prev) =>
                    prev.some((n) => n.toLowerCase() === typed.toLowerCase())
                      ? prev
                      : [...prev, typed],
                  );
                }
                setCompanySearch('');
              }}
            />
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

      <StudentListModal
        open={studentModal}
        onClose={closeStudentModal}
        classId={selectedClass?.id ?? null}
        trainInfoId={selectedClass?.trainInfoId ?? null}
        workTypeTitle={selectedClass?.workTypeTitle}
        workTypeLevel={selectedClass?.workTypeLevel}
        workTypeId={selectedClass?.workTypeId}
        mode={studentModalMode}
        onProfileChanged={() => queryClient.invalidateQueries({ queryKey: ['info-classes'] })}
      />
      <StaffModal open={staffModal} onClose={() => setStaffModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null}
        tag="teacher" title="教务人员" />
      <StaffModal open={chargeModal} onClose={() => setChargeModal(false)}
        classId={selectedClass?.id ?? null} trainInfoId={selectedClass?.trainInfoId ?? null}
        tag="fuzhe" title="负责人" />
      <ClassRolesModal
        open={rolesModal}
        onClose={() => setRolesModal(false)}
        classId={selectedClass?.id ?? null}
      />
      <TrainTaskModal open={taskModal} onClose={() => setTaskModal(false)}
        classId={selectedClass?.id ?? null} kejianOptions={kejianOptions} />
    </div>
  );
}
