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
      for (const id of payload.ids) {
        await trainApi.assignClassRole(classId, {
          roleCode: payload.roleCode,
          subjectType: payload.subjectType,
          subjectId: id,
        });
      }
    },
    onSuccess: () => {
      message.success('指派成功（开班准备与跟班已同步）');
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
      width: 100,
      align: 'center',
      render: (_, row) =>
        row.roleCode === 'monitor' ? (
          <Typography.Text type="secondary">花名册</Typography.Text>
        ) : (
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
        ),
    },
  ];

  return (
    <div>
      {showAlert && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="班级角色与开班准备共用 train_class_role，两处维护实时同步"
          description={
            <>
              {unboundCount > 0
                ? `有 ${unboundCount} 人未绑定 App 账号，指派后需确保可登录。`
                : '可从师资库指派理论老师、实操老师、教务人员。'}
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
              setImportPage(p);
              setImportPageSize(size);
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
