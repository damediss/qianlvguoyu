/**
 * 班级角色维护（train_class_role）
 * 开班准备弹窗与跟班工作台共用，数据同一真相源。
 */
import { useDeferredValue, useState } from 'react';
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainClassRole } from '../../types/training';

type ApiId = number | string;

const ROLE_FILTER_OPTIONS = [
  { value: 'all', label: '全部角色' },
  { value: 'follow_staff', label: '教务人员' },
  { value: 'lead_teacher', label: '理论老师' },
  { value: 'assistant', label: '实操老师' },
  { value: 'monitor', label: '班长' },
];

const ASSIGN_ROLE_OPTIONS = [
  { value: 'lead_teacher', label: '理论老师' },
  { value: 'assistant', label: '实操老师' },
  { value: 'follow_staff', label: '教务人员' },
];

function matchesRoleFilter(roleCode: string, filter: string): boolean {
  if (filter === 'all') return true;
  if (filter === 'follow_staff') return roleCode === 'follow_staff' || roleCode === 'head_teacher';
  return roleCode === filter;
}

function invalidateRoleQueries(qc: ReturnType<typeof useQueryClient>, classId: ApiId) {
  void qc.invalidateQueries({ queryKey: ['class-roles', classId] });
  void qc.invalidateQueries({ queryKey: ['follow-role-summary', classId] });
  void qc.invalidateQueries({ queryKey: ['class-role-summary', classId] });
}

function isValidIdCard(v?: string | null): boolean {
  const s = (v || '').trim();
  return /^[0-9Xx]{15}$/.test(s) || /^[0-9Xx]{18}$/.test(s);
}

function isValidPhone(v?: string | null): boolean {
  return /^1\d{10}$/.test((v || '').replace(/\s/g, ''));
}

export type ClassRolesPanelProps = {
  classId: ApiId;
  /** 是否展示顶部说明 Alert，默认 true */
  showAlert?: boolean;
  /** 嵌入跟班工作台时的额外提示 */
  contextHint?: string;
};

/** 内嵌面板：列表 + 指派/卸任（与开班准备同源 API） */
export function ClassRolesPanel({ classId, showAlert = true, contextHint }: ClassRolesPanelProps) {
  const qc = useQueryClient();
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [importOpen, setImportOpen] = useState(false);
  const [assignRoleCode, setAssignRoleCode] = useState('lead_teacher');
  const [selectedTeacherIds, setSelectedTeacherIds] = useState<Array<number | string>>([]);
  const [importPage, setImportPage] = useState(1);
  const [importPageSize, setImportPageSize] = useState(8);
  const [teacherKeyword, setTeacherKeyword] = useState('');
  const deferredTeacherKeyword = useDeferredValue(teacherKeyword.trim());

  const { data: roles = [], isLoading, refetch, isFetching } = useQuery({
    queryKey: ['class-roles', classId],
    queryFn: () => trainApi.listClassRoles(classId),
    enabled: !!classId,
  });

  const { data: teacherPage, isFetching: teachersLoading } = useQuery({
    queryKey: ['class-roles-teachers', importPage, importPageSize, deferredTeacherKeyword],
    queryFn: () =>
      trainApi.pageTeacherBanks({
        page: importPage,
        pageSize: importPageSize,
        keyword: deferredTeacherKeyword || undefined,
      }),
    enabled: importOpen,
  });

  const assignMu = useMutation({
    mutationFn: async (payload: {
      roleCode: string;
      ids: Array<number | string>;
      subjectType: string;
    }) => {
      const results: TrainClassRole[] = [];
      for (const id of payload.ids) {
        results.push(
          await trainApi.assignClassRole(classId, {
            roleCode: payload.roleCode,
            subjectType: payload.subjectType,
            subjectId: id,
          }),
        );
      }
      return results;
    },
    onSuccess: (results) => {
      const ensured = results.some((r) => r.bindStatus === 'ensured');
      message.success(
        ensured
          ? '指派成功，已开通 App 账号，默认密码 123123，请通知老师首次登录后修改'
          : '指派成功（开班准备与跟班已同步）',
      );
      setImportOpen(false);
      setSelectedTeacherIds([]);
      invalidateRoleQueries(qc, classId);
    },
    onError: (e: Error) => message.error(e.message || '指派失败'),
  });

  const endMu = useMutation({
    mutationFn: (roleId: string) => trainApi.endClassRole(classId, roleId),
    onSuccess: () => {
      message.success('已卸任（开班准备与跟班已同步）');
      invalidateRoleQueries(qc, classId);
    },
    onError: (e: Error) => message.error(e.message || '卸任失败'),
  });

  const ensureMu = useMutation({
    mutationFn: (roleId: string) => trainApi.ensureClassRoleAppAccount(classId, roleId),
    onSuccess: (row) => {
      message.success(
        row.bindStatus === 'ensured'
          ? '已开通 App 账号，默认密码 123123，请通知老师首次登录后修改'
          : '已绑定 App 账号',
      );
      invalidateRoleQueries(qc, classId);
    },
    onError: (e: Error) => message.error(e.message || '开通失败'),
  });

  const filtered = roles.filter((r) => matchesRoleFilter(r.roleCode, roleFilter));
  const unboundCount = roles.filter((r) => r.unboundWarning).length;

  const columns: ColumnsType<TrainClassRole> = [
    { title: '角色', dataIndex: 'roleName', width: 120 },
    { title: '姓名', dataIndex: 'displayName', ellipsis: true },
    { title: '手机', dataIndex: 'phone', width: 130, render: (v?: string) => v || '—' },
    {
      title: 'App账号',
      width: 100,
      align: 'center',
      render: (_, row) =>
        row.unboundWarning ? (
          <Typography.Text type="warning">未绑定</Typography.Text>
        ) : (
          <Typography.Text type="success">已绑定</Typography.Text>
        ),
    },
    {
      title: '操作',
      width: 160,
      align: 'center',
      render: (_, row) =>
        row.roleCode === 'monitor' ? (
          <Typography.Text type="secondary">花名册</Typography.Text>
        ) : (
          <Space size={0}>
            {row.unboundWarning ? (
              <Button
                type="link"
                size="small"
                loading={ensureMu.isPending}
                onClick={() => ensureMu.mutate(row.id)}
              >
                开通账号
              </Button>
            ) : null}
            <Button
              type="link"
              danger
              size="small"
              loading={endMu.isPending}
              onClick={() =>
                Modal.confirm({
                  title: '确认卸任？',
                  content: `将解除「${row.roleName}」：${row.displayName || '—'}`,
                  onOk: () => endMu.mutateAsync(row.id),
                })
              }
            >
              卸任
            </Button>
          </Space>
        ),
    },
  ];

  return (
    <div>
      {showAlert && (
        <Alert
          type={unboundCount > 0 ? 'warning' : 'info'}
          showIcon
          style={{ marginBottom: 12 }}
          message="班级角色与开班准备共用 train_class_role，两处维护实时同步"
          description={
            <>
              {unboundCount > 0
                ? `有 ${unboundCount} 人未绑定 App 账号，请点「开通账号」或先补全师资库身份证/手机号后再指派。`
                : '可从师资库指派理论老师、实操老师、教务人员；指派时将自动开通 App 账号。'}
              {' '}
              班长请在开班准备「学员列表」设置是否班长（系统自动同步）。
              {contextHint ? ` ${contextHint}` : ''}
            </>
          }
        />
      )}

      <Space style={{ marginBottom: 12 }} wrap>
        <Select
          value={roleFilter}
          style={{ width: 160 }}
          onChange={setRoleFilter}
          options={ROLE_FILTER_OPTIONS}
        />
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setAssignRoleCode('lead_teacher');
            setTeacherKeyword('');
            setImportPage(1);
            setSelectedTeacherIds([]);
            setImportOpen(true);
          }}
        >
          从师资库指派
        </Button>
        <Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => void refetch()}>
          刷新
        </Button>
        <Typography.Text type="secondary">班长请在学员列表设置</Typography.Text>
      </Space>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={filtered}
        columns={columns}
        pagination={false}
        locale={{ emptyText: '暂无角色，请从师资库指派或设置班长' }}
        size="small"
      />

      <Modal
        title="从师资库指派"
        open={importOpen}
        onCancel={() => setImportOpen(false)}
        onOk={() => {
          if (!selectedTeacherIds.length) {
            message.warning('请选择人员');
            return;
          }
          const teachers = teacherPage?.items ?? [];
          const selected = teachers.filter((t) =>
            selectedTeacherIds.some((id) => String(id) === String(t.id)),
          );
          // 仅校验当前页已加载项；跨页选中若缺资料由后端硬失败兜底
          for (const t of selected) {
            if (!isValidIdCard(t.idCardNumber) || String(t.idCardNumber).trim().length !== 18) {
              message.error(`「${t.name || '未命名'}」请先在师资库补全 18 位身份证后再指派`);
              return;
            }
            if (!isValidPhone(t.phone)) {
              message.error(`「${t.name || '未命名'}」请先在师资库补全 11 位手机号后再指派`);
              return;
            }
          }
          return assignMu.mutateAsync({
            roleCode: assignRoleCode,
            subjectType: 'teacher_bank',
            ids: selectedTeacherIds,
          });
        }}
        confirmLoading={assignMu.isPending}
        width={720}
        destroyOnHidden
      >
        <Form.Item label="角色">
          <Select
            value={assignRoleCode}
            onChange={setAssignRoleCode}
            options={ASSIGN_ROLE_OPTIONS}
          />
        </Form.Item>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="须具备 18 位身份证与 11 位手机号；指派成功将自动开通 App 账号（默认密码 123123）"
        />
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索姓名 / 手机 / 身份证"
          value={teacherKeyword}
          onChange={(e) => {
            setTeacherKeyword(e.target.value);
            setImportPage(1);
          }}
          style={{ marginBottom: 12 }}
        />
        <Table
          rowKey="id"
          size="small"
          loading={teachersLoading}
          dataSource={teacherPage?.items ?? []}
          columns={[
            { title: '姓名', dataIndex: 'name', width: 100 },
            {
              title: '身份证号',
              width: 170,
              render: (_, row) => {
                const v = row.idCardNumber?.trim();
                if (v && (/^[0-9Xx]{15}$/.test(v) || /^[0-9Xx]{18}$/.test(v))) return v;
                return '—';
              },
            },
            { title: '手机', dataIndex: 'phone', width: 130, render: (v?: string) => v || '—' },
          ]}
          rowSelection={{
            selectedRowKeys: selectedTeacherIds,
            preserveSelectedRowKeys: true,
            onChange: (keys) => setSelectedTeacherIds(keys as Array<number | string>),
          }}
          pagination={{
            current: importPage,
            pageSize: importPageSize,
            total: teacherPage?.total ?? 0,
            showSizeChanger: true,
            pageSizeOptions: ['8', '20', '50', '100'],
            showTotal: (t) => `共 ${t} 人`,
            onChange: (p, size) => {
              if (typeof size === 'number' && size > 0 && size !== importPageSize) {
                setImportPageSize(size);
                setImportPage(1);
              } else {
                setImportPage(p);
              }
            },
          }}
        />
      </Modal>
    </div>
  );
}

/** 开班准备用弹窗壳 */
export function ClassRolesModal({
  open,
  onClose,
  classId,
}: {
  open: boolean;
  onClose: () => void;
  classId: ApiId | null;
}) {
  return (
    <Modal title="班级角色" open={open} onCancel={onClose} footer={null} width="64%" destroyOnHidden>
      {classId != null ? (
        <ClassRolesPanel classId={classId} />
      ) : (
        <Typography.Text type="secondary">未选择班级</Typography.Text>
      )}
    </Modal>
  );
}
