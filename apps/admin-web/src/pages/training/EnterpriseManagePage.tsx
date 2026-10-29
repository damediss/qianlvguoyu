import { useEffect, useMemo, useState } from 'react';
import {
  Button, Card, Drawer, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Tree, Typography, message,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, BankOutlined,
  UserOutlined, KeyOutlined, SettingOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import type {
  EnterpriseAccount,
  EnterpriseAccountCreateReq,
  EnterpriseAccountUpdateReq,
  SystemCompany,
  SystemCompanySaveReq,
} from '../../types/training';

const STATUS_OPTIONS = [
  { value: 'enabled', label: '启用' },
  { value: 'disabled', label: '禁用' },
];

/** 雪花 Long 超过 JS 安全整数，Select 必须用字符串 value，否则只显示数字 ID */
function roleIdStr(id: number | string | null | undefined): string {
  return id == null ? '' : String(id);
}

function buildRoleSelectOptions(
  roles: Array<{ id: number | string; code?: string; name?: string; defaultRole?: boolean }>,
  selectedIds?: Array<number | string>,
  selectedCodes?: string[],
): Array<{ value: string; label: string }> {
  const opts: Array<{ value: string; label: string }> = [];
  const known = new Set<string>();
  for (const r of roles) {
    const value = roleIdStr(r.id);
    if (!value || known.has(value)) continue;
    known.add(value);
    const name = r.name || r.code || value;
    opts.push({
      value,
      label: r.defaultRole ? `${name}（默认）` : name,
    });
  }
  (selectedIds ?? []).forEach((id, i) => {
    const value = roleIdStr(id);
    if (!value || known.has(value)) return;
    known.add(value);
    opts.push({
      value,
      label: selectedCodes?.[i] || `角色 ${value}`,
    });
  });
  return opts;
}

function isPlatformAdminUser(roles: string[] | undefined, userType: string | undefined, username: string | undefined) {
  const roleSet = new Set((roles ?? []).map((r) => String(r).toUpperCase()));
  return (
    roleSet.has('PLATFORM_ADMIN')
    || roleSet.has('SUPER_ADMIN')
    || userType === 'admin'
    || userType === 'platform_admin'
    || username === 'admin'
  );
}

export function EnterpriseManagePage() {
  const queryClient = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const currentUser = useAuthStore((s) => s.currentUser);
  const isPlatformAdmin = isPlatformAdminUser(
    currentUser?.roles,
    currentUser?.userType,
    currentUser?.username,
  );
  const canCreate = hasPerm('train:company:create') || isPlatformAdmin;
  const canUpdate = hasPerm('train:company:update') || isPlatformAdmin;
  const canDelete = hasPerm('train:company:delete') || isPlatformAdmin;
  // 开设账号：显式权限 / 有建企权限 / 平台管理员（避免种子未刷时超管看不到入口）
  const canAccount =
    hasPerm('train:company:account')
    || hasPerm('train:company:create')
    || isPlatformAdmin;
  const canFeature =
    hasPerm('train:company:feature')
    || hasPerm('train:company:update')
    || hasPerm('train:company:create')
    || isPlatformAdmin;

  const [form] = Form.useForm<SystemCompanySaveReq>();
  const [accountForm] = Form.useForm<EnterpriseAccountCreateReq>();
  const [editAccountForm] = Form.useForm<EnterpriseAccountUpdateReq>();
  const [pwdForm] = Form.useForm<{ newPassword: string }>();
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<SystemCompany | null>(null);

  const [accountCompany, setAccountCompany] = useState<SystemCompany | null>(null);
  const [accountKeyword, setAccountKeyword] = useState('');
  const [accountPage, setAccountPage] = useState({ page: 1, pageSize: 10 });
  const [createAccountOpen, setCreateAccountOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<EnterpriseAccount | null>(null);
  const [pwdTarget, setPwdTarget] = useState<EnterpriseAccount | null>(null);

  const [featureCompany, setFeatureCompany] = useState<SystemCompany | null>(null);
  const [featureConstrain, setFeatureConstrain] = useState(true);
  const [featureMenuCodes, setFeatureMenuCodes] = useState<string[]>([]);
  const [featureRoleIds, setFeatureRoleIds] = useState<string[]>([]);

  const invalidateAccounts = () => {
    void queryClient.invalidateQueries({ queryKey: ['enterprise-accounts', accountCompany?.id] });
  };

  const { data = [], isLoading, refetch } = useQuery({
    queryKey: ['system-companies', searchKeyword],
    queryFn: () => trainApi.listSystemCompanies(searchKeyword || undefined),
  });

  const rolesQuery = useQuery({
    queryKey: ['enterprise-account-roles', accountCompany?.id],
    queryFn: () => trainApi.listEnterpriseAccountRoles(accountCompany!.id),
    enabled: canAccount && !!accountCompany,
  });

  const featureOptionsQuery = useQuery({
    queryKey: ['enterprise-feature-options'],
    queryFn: () => trainApi.getEnterpriseFeatureOptions(),
    enabled: canFeature && !!featureCompany,
  });

  const featureConfigQuery = useQuery({
    queryKey: ['enterprise-feature-config', featureCompany?.id],
    queryFn: () => trainApi.getEnterpriseFeatureConfig(featureCompany!.id),
    enabled: canFeature && !!featureCompany,
  });

  const accountsQuery = useQuery({
    queryKey: [
      'enterprise-accounts',
      accountCompany?.id,
      accountPage.page,
      accountPage.pageSize,
      accountKeyword,
    ],
    queryFn: () =>
      trainApi.listEnterpriseAccounts(accountCompany!.id, {
        page: accountPage.page,
        pageSize: accountPage.pageSize,
        keyword: accountKeyword || undefined,
      }),
    enabled: !!accountCompany,
  });

  const createMu = useMutation({
    mutationFn: (payload: SystemCompanySaveReq) => trainApi.createSystemCompany(payload),
    onSuccess: () => {
      message.success('创建成功');
      closeModal();
      void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
      void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const updateMu = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: SystemCompanySaveReq }) =>
      trainApi.updateSystemCompany(id, payload),
    onSuccess: () => {
      message.success('更新成功');
      closeModal();
      void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
      void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSystemCompany(id),
    onSuccess: () => {
      message.success('删除成功');
      void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
      void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const createAccountMu = useMutation({
    mutationFn: (payload: EnterpriseAccountCreateReq) =>
      trainApi.createEnterpriseAccount(accountCompany!.id, payload),
    onSuccess: () => {
      message.success('账号已创建，可使用同一租户登录管理端');
      setCreateAccountOpen(false);
      accountForm.resetFields();
      invalidateAccounts();
    },
    onError: (e: Error) => message.error(e.message || '开设账号失败'),
  });

  const updateAccountMu = useMutation({
    mutationFn: (payload: EnterpriseAccountUpdateReq) =>
      trainApi.updateEnterpriseAccount(accountCompany!.id, editingAccount!.id, payload),
    onSuccess: () => {
      message.success('账号已更新');
      setEditingAccount(null);
      editAccountForm.resetFields();
      invalidateAccounts();
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const statusAccountMu = useMutation({
    mutationFn: ({ userId, status }: { userId: number; status: 'enabled' | 'disabled' }) =>
      trainApi.setEnterpriseAccountStatus(accountCompany!.id, userId, status),
    onSuccess: (_data, vars) => {
      message.success(vars.status === 'disabled' ? '已停用' : '已启用');
      invalidateAccounts();
    },
    onError: (e: Error) => message.error(e.message || '操作失败'),
  });

  const resetPwdMu = useMutation({
    mutationFn: (newPassword: string) =>
      trainApi.resetEnterpriseAccountPassword(accountCompany!.id, pwdTarget!.id, { newPassword }),
    onSuccess: () => {
      message.success('密码已重置');
      setPwdTarget(null);
      pwdForm.resetFields();
    },
    onError: (e: Error) => message.error(e.message || '重置失败'),
  });

  const saveFeatureMu = useMutation({
    mutationFn: () =>
      trainApi.saveEnterpriseFeatureConfig(featureCompany!.id, {
        menuCodes: featureMenuCodes,
        roleIds: featureRoleIds,
        constrainEnabled: featureConstrain,
      }),
    onSuccess: () => {
      message.success('功能配置已保存；企业账号重新登录后生效');
      void queryClient.invalidateQueries({ queryKey: ['enterprise-feature-config', featureCompany?.id] });
      void queryClient.invalidateQueries({ queryKey: ['enterprise-account-roles'] });
      setFeatureCompany(null);
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
    form.resetFields();
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, status: 'enabled' });
    setModalOpen(true);
  };

  const openEdit = (row: SystemCompany) => {
    setEditing(row);
    form.setFieldsValue({
      name: row.name,
      sortOrder: row.sortOrder ?? 0,
      status: row.status || 'enabled',
    });
    setModalOpen(true);
  };

  const openAccounts = (row: SystemCompany) => {
    setAccountCompany(row);
    setAccountKeyword('');
    setAccountPage({ page: 1, pageSize: 10 });
  };

  const openCreateAccount = () => {
    accountForm.resetFields();
    const defaultRole = rolesQuery.data?.find((r) => r.defaultRole);
    accountForm.setFieldsValue({
      status: 'enabled',
      // Select value 用字符串，避免雪花 ID 精度丢失后只显示数字
      roleIds: defaultRole ? [roleIdStr(defaultRole.id) as unknown as number] : undefined,
    });
    setCreateAccountOpen(true);
  };

  const openEditAccount = (row: EnterpriseAccount) => {
    setEditingAccount(row);
    editAccountForm.setFieldsValue({
      displayName: row.displayName,
      phone: row.phone,
      email: row.email,
      status: row.status === 'disabled' || row.status === 'locked' ? row.status : 'enabled',
      roleIds: row.roleIds?.length
        ? (row.roleIds.map(roleIdStr) as unknown as number[])
        : undefined,
    });
  };

  const accountRoleOptions = useMemo(
    () =>
      buildRoleSelectOptions(
        rolesQuery.data ?? [],
        editingAccount?.roleIds,
        editingAccount?.roleCodes,
      ),
    [rolesQuery.data, editingAccount?.roleIds, editingAccount?.roleCodes],
  );

  const featureRoleOptions = useMemo(
    () => buildRoleSelectOptions(featureOptionsQuery.data?.roles ?? [], featureRoleIds),
    [featureOptionsQuery.data?.roles, featureRoleIds],
  );

  const openFeatureConfig = (row: SystemCompany) => {
    setFeatureCompany(row);
    setFeatureConstrain(true);
    setFeatureMenuCodes([]);
    setFeatureRoleIds([]);
  };

  useEffect(() => {
    const cfg = featureConfigQuery.data;
    if (!cfg || !featureCompany) return;
    setFeatureConstrain(!!cfg.constrainEnabled);
    setFeatureMenuCodes(cfg.menuCodes ?? []);
    setFeatureRoleIds((cfg.roleIds ?? []).map(roleIdStr).filter(Boolean));
  }, [featureConfigQuery.data, featureCompany]);

  const featureMenuTree: DataNode[] = useMemo(() => {
    const menus = featureOptionsQuery.data?.menus ?? [];
    if (!menus.length) return [];
    const byId = new Map(menus.map((m) => [m.id, m]));
    const childrenOf = new Map<number, typeof menus>();
    menus.forEach((m) => {
      const pid = m.parentId && byId.has(m.parentId) ? m.parentId : 0;
      const list = childrenOf.get(pid) ?? [];
      list.push(m);
      childrenOf.set(pid, list);
    });
    const build = (parentId: number): DataNode[] =>
      (childrenOf.get(parentId) ?? []).map((m) => {
        const kids = build(m.id);
        return {
          key: m.code,
          title: m.name,
          children: kids.length ? kids : undefined,
        };
      });
    return build(0);
  }, [featureOptionsQuery.data]);

  const columns: ColumnsType<SystemCompany> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 90 },
      { title: '编码', dataIndex: 'code', width: 180 },
      { title: '企业名称', dataIndex: 'name', width: 200, ellipsis: true },
      {
        title: '所属机构',
        dataIndex: 'parentName',
        width: 180,
        ellipsis: true,
        render: (v?: string | null, row?: SystemCompany) =>
          v || (row?.parentId && row.parentId > 0 ? `机构#${row.parentId}` : '—'),
      },
      { title: '排序', dataIndex: 'sortOrder', width: 80, align: 'center' },
      {
        title: '状态',
        dataIndex: 'status',
        width: 90,
        align: 'center',
        render: (v: string) =>
          v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>禁用</Tag>,
      },
      {
        title: '操作',
        key: 'act',
        width: 300,
        fixed: 'right',
        render: (_, row) => (
          <Space size="small" wrap>
            <Button type="link" size="small" icon={<UserOutlined />} onClick={() => openAccounts(row)}>
              账号
            </Button>
            {canFeature ? (
              <Button
                type="link"
                size="small"
                icon={<SettingOutlined />}
                onClick={() => openFeatureConfig(row)}
              >
                功能配置
              </Button>
            ) : null}
            {canUpdate ? (
              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>
                编辑
              </Button>
            ) : null}
            {canDelete ? (
              <Popconfirm title="确定删除该企业？" onConfirm={() => deleteMu.mutate(row.id)}>
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            ) : null}
          </Space>
        ),
      },
    ],
    [canDelete, canFeature, canUpdate, deleteMu],
  );

  const accountColumns: ColumnsType<EnterpriseAccount> = useMemo(
    () => [
      { title: '登录账号', dataIndex: 'username', width: 140 },
      { title: '昵称', dataIndex: 'displayName', width: 120 },
      {
        title: '角色',
        dataIndex: 'roleCodes',
        ellipsis: true,
        render: (codes?: string[]) =>
          codes?.length ? codes.map((c) => <Tag key={c}>{c}</Tag>) : '-',
      },
      {
        title: '手机',
        dataIndex: 'phone',
        width: 120,
        render: (v?: string) => v || '-',
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 80,
        render: (v?: string) =>
          v === 'enabled' ? <Tag color="success">正常</Tag>
            : v === 'disabled' ? <Tag color="error">停用</Tag>
              : <Tag>{v || '-'}</Tag>,
      },
      {
        title: '操作',
        key: 'act',
        width: 220,
        render: (_, row) =>
          canAccount ? (
            <Space size={0} wrap>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => openEditAccount(row)}
              >
                编辑
              </Button>
              {row.status === 'disabled' ? (
                <Popconfirm
                  title="确定启用该账号？"
                  onConfirm={() => statusAccountMu.mutate({ userId: row.id, status: 'enabled' })}
                >
                  <Button type="link" size="small">启用</Button>
                </Popconfirm>
              ) : (
                <Popconfirm
                  title="确定停用该账号？停用后将无法登录"
                  onConfirm={() => statusAccountMu.mutate({ userId: row.id, status: 'disabled' })}
                >
                  <Button type="link" size="small" danger>停用</Button>
                </Popconfirm>
              )}
              <Button
                type="link"
                size="small"
                icon={<KeyOutlined />}
                onClick={() => {
                  setPwdTarget(row);
                  pwdForm.resetFields();
                }}
              >
                重置密码
              </Button>
            </Space>
          ) : null,
      },
    ],
    [canAccount, pwdForm, statusAccountMu],
  );

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>
            <BankOutlined style={{ marginRight: 8 }} />
            企业管理
          </Typography.Title>
          <Typography.Text type="secondary">
            维护培训关联企业；可为下属/本企业开设管理端登录账号（同一租户登录）
          </Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="按名称或编码搜索"
            prefix={<SearchOutlined />}
            value={keyword}
            allowClear
            style={{ width: 240 }}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => setSearchKeyword(keyword.trim())}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => setSearchKeyword(keyword.trim())}
          >
            搜索
          </Button>
          {canCreate ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新增企业
            </Button>
          ) : null}
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            刷新
          </Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data}
          scroll={{ x: 900 }}
          pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        />
      </Card>

      <Modal
        title={editing ? '编辑企业' : '新增企业'}
        open={modalOpen}
        onCancel={closeModal}
        onOk={() => form.submit()}
        confirmLoading={createMu.isPending || updateMu.isPending}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => {
            if (editing) {
              updateMu.mutate({ id: editing.id, payload: values });
            } else {
              createMu.mutate(values);
            }
          }}
        >
          <Form.Item
            name="name"
            label="企业名称"
            rules={[{ required: true, message: '请输入企业名称' }]}
          >
            <Input maxLength={128} placeholder="请输入企业名称" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={accountCompany ? `企业账号 · ${accountCompany.name}` : '企业账号'}
        width={720}
        open={!!accountCompany}
        onClose={() => setAccountCompany(null)}
        destroyOnClose
        extra={
          canAccount ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreateAccount}>
              开设账号
            </Button>
          ) : null
        }
      >
        <Space style={{ marginBottom: 12 }} wrap>
          <Input
            placeholder="搜索登录账号/昵称/手机"
            allowClear
            style={{ width: 240 }}
            value={accountKeyword}
            onChange={(e) => {
              setAccountKeyword(e.target.value);
              setAccountPage((p) => ({ ...p, page: 1 }));
            }}
          />
          <Typography.Text type="secondary">
            登录：选择同一租户，使用下方登录账号和密码进入管理端
          </Typography.Text>
        </Space>
        <Table
          rowKey="id"
          size="small"
          loading={accountsQuery.isLoading}
          columns={accountColumns}
          dataSource={accountsQuery.data?.items ?? []}
          pagination={{
            current: accountPage.page,
            pageSize: accountPage.pageSize,
            total: accountsQuery.data?.total ?? 0,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, pageSize) => setAccountPage({ page, pageSize }),
          }}
        />
      </Drawer>

      <Modal
        title="开设企业账号"
        open={createAccountOpen}
        onCancel={() => setCreateAccountOpen(false)}
        onOk={() => accountForm.submit()}
        confirmLoading={createAccountMu.isPending}
        destroyOnClose
      >
        <Form
          form={accountForm}
          layout="vertical"
          onFinish={(values) => createAccountMu.mutate(values)}
        >
          <Form.Item label="所属企业">
            <Input value={accountCompany?.name} disabled />
          </Form.Item>
          <Form.Item
            name="username"
            label="登录账号"
            rules={[{ required: true, message: '请输入登录账号' }]}
          >
            <Input maxLength={64} autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="password"
            label="初始密码"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 8, message: '至少 8 位' },
            ]}
          >
            <Input.Password maxLength={128} autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="displayName"
            label="昵称"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input maxLength={32} />
          </Form.Item>
          <Form.Item name="roleIds" label="角色">
            <Select
              mode="multiple"
              allowClear
              optionFilterProp="label"
              placeholder="默认企业运营角色 TRAIN_ENTERPRISE"
              options={accountRoleOptions}
              loading={rolesQuery.isLoading}
            />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="enabled">
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingAccount ? `编辑账号 · ${editingAccount.username}` : '编辑账号'}
        open={!!editingAccount}
        onCancel={() => {
          setEditingAccount(null);
          editAccountForm.resetFields();
        }}
        onOk={() => editAccountForm.submit()}
        confirmLoading={updateAccountMu.isPending}
        destroyOnClose
      >
        <Form
          form={editAccountForm}
          layout="vertical"
          onFinish={(values) => {
            updateAccountMu.mutate({
              ...values,
              roleIds: values.roleIds?.length
                ? values.roleIds
                : (editingAccount?.roleIds ?? []),
            });
          }}
        >
          <Form.Item label="登录账号">
            <Input value={editingAccount?.username} disabled />
          </Form.Item>
          <Form.Item
            name="displayName"
            label="昵称"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input maxLength={32} />
          </Form.Item>
          <Form.Item name="roleIds" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select
              mode="multiple"
              optionFilterProp="label"
              options={accountRoleOptions}
              loading={rolesQuery.isLoading}
            />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select
              options={[
                { value: 'enabled', label: '启用' },
                { value: 'disabled', label: '停用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={pwdTarget ? `重置密码 · ${pwdTarget.username}` : '重置密码'}
        open={!!pwdTarget}
        onCancel={() => setPwdTarget(null)}
        onOk={() => pwdForm.submit()}
        confirmLoading={resetPwdMu.isPending}
        destroyOnClose
      >
        <Form
          form={pwdForm}
          layout="vertical"
          onFinish={(values) => resetPwdMu.mutate(values.newPassword)}
        >
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '至少 8 位' },
            ]}
          >
            <Input.Password maxLength={128} autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={featureCompany ? `功能配置 · ${featureCompany.name}` : '功能配置'}
        width={560}
        open={!!featureCompany}
        onClose={() => setFeatureCompany(null)}
        destroyOnClose
        extra={
          <Button
            type="primary"
            loading={saveFeatureMu.isPending}
            onClick={() => saveFeatureMu.mutate()}
          >
            保存
          </Button>
        }
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div>
            <Space>
              <Typography.Text strong>启用功能约束</Typography.Text>
              <Switch checked={featureConstrain} onChange={setFeatureConstrain} />
            </Space>
            <div>
              <Typography.Text type="secondary">
                开启后：该企业账号仅可见勾选菜单，且开户只能选下方角色包。关闭则不限制（沿用账号自身角色权限）。
              </Typography.Text>
            </div>
          </div>

          <div>
            <Typography.Text strong>菜单 / 模块</Typography.Text>
            <div style={{ marginTop: 8, maxHeight: 360, overflow: 'auto', border: '1px solid #f0f0f0', padding: 8 }}>
              <Tree
                checkable
                defaultExpandAll
                treeData={featureMenuTree}
                checkedKeys={featureMenuCodes}
                onCheck={(checked) => {
                  const keys = Array.isArray(checked) ? checked : checked.checked;
                  setFeatureMenuCodes(keys.map(String));
                }}
              />
            </div>
          </div>

          <div>
            <Typography.Text strong>可授角色包</Typography.Text>
            <Select
              mode="multiple"
              style={{ width: '100%', marginTop: 8 }}
              placeholder="选择该企业开户时可分配的角色"
              optionFilterProp="label"
              value={featureRoleIds}
              onChange={setFeatureRoleIds}
              options={featureRoleOptions}
              loading={featureOptionsQuery.isLoading}
            />
          </div>
        </Space>
      </Drawer>
    </div>
  );
}
