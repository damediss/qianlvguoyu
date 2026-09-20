import { DeleteOutlined, EditOutlined, MinusCircleOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Result,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { treeWorkTypes } from '../../api/exam';
import {
  createCompetitionReferee,
  deleteCompetitionReferee,
  getCompetitionRefereeForEdit,
  pageCompetitionReferees,
  updateCompetitionReferee,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type { ExamWorkType } from '../../types/exam';
import type {
  CompetitionId,
  CompetitionReferee,
  CompetitionRefereeCreateRequest,
  CompetitionRefereeQualificationRequest,
} from '../../types/competition';
import './CompetitionEventManagementPage.css';

type RefereeFormValues = CompetitionRefereeCreateRequest;

const LEVEL_OPTIONS = [
  { value: '5', label: '初级工' },
  { value: '4', label: '中级工' },
  { value: '3', label: '高级工' },
  { value: '2', label: '技师' },
  { value: '1', label: '高级技师' },
];

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function validIdCard(value: string) {
  const normalized = value.trim().toUpperCase();
  if (!/^\d{17}[0-9X]$/.test(normalized)) return false;
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const codes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];
  const sum = weights.reduce((total, weight, index) => total + Number(normalized[index]) * weight, 0);
  return codes[sum % 11] === normalized[17];
}

function flattenWorkTypes(nodes: ExamWorkType[]): ExamWorkType[] {
  return nodes.flatMap((node) => node.children?.length ? flattenWorkTypes(node.children) : [node]);
}

export function CompetitionRefereeManagementPage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:referee:view');
  const canCreate = permissions.includes('competition:referee:create');
  const canUpdate = permissions.includes('competition:referee:update');
  const canDelete = permissions.includes('competition:referee:delete');
  const queryClient = useQueryClient();
  const [workTypeId, setWorkTypeId] = useState<CompetitionId>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<CompetitionId | null>(null);
  const [form] = Form.useForm<RefereeFormValues>();

  const workTypesQuery = useQuery({
    queryKey: ['exam-work-types', 'competition-referee-management'],
    queryFn: treeWorkTypes,
    enabled: canView,
  });
  const refereesQuery = useQuery({
    queryKey: ['competition-referees', workTypeId, keyword, page, pageSize],
    queryFn: () => pageCompetitionReferees({
      page,
      pageSize,
      workTypeId,
      keyword: keyword || undefined,
    }),
    enabled: canView,
  });
  const editQuery = useQuery({
    queryKey: ['competition-referee', editingId],
    queryFn: () => getCompetitionRefereeForEdit(editingId!),
    enabled: modalOpen && editingId !== null,
  });

  const workTypes = useMemo(
    () => flattenWorkTypes(workTypesQuery.data ?? []),
    [workTypesQuery.data],
  );
  const workTypeOptions = useMemo(() => {
    // The referee API serializes Long IDs as strings while the exam tree
    // returns Integer IDs as numbers. AntD Select compares values strictly.
    const options = new Map<string, { label: string; value: string }>();
    workTypes.forEach((item) => {
      const value = idValue(item.id);
      if (value) options.set(value, { label: item.title || value, value });
    });
    // Keep the edit form readable even if the work type tree arrives later
    // than the referee detail, or a historical work type is no longer in it.
    (editQuery.data?.qualifications ?? []).forEach((item) => {
      const value = idValue(item.workTypeId);
      if (value && item.workTypeName && !options.has(value)) {
        options.set(value, { label: item.workTypeName, value });
      }
    });
    return Array.from(options.values());
  }, [editQuery.data?.qualifications, workTypes]);

  useEffect(() => {
    if (!editQuery.data || !modalOpen || editingId === null) return;
    form.setFieldsValue({
      qualifications: (editQuery.data.qualifications ?? []).map((item) => ({
        // Keep the edit value type consistent with the normalized options.
        workTypeId: idValue(item.workTypeId),
        levelFromCode: item.levelFromCode || item.levelCode || undefined,
        levelToCode: item.levelToCode || item.levelCode || undefined,
      })),
      name: editQuery.data.name,
      idCardNo: editQuery.data.idCardNo,
      mobile: editQuery.data.mobile,
    });
  }, [editQuery.data, editingId, form, modalOpen]);

  const createMutation = useMutation({
    mutationFn: (payload: CompetitionRefereeCreateRequest) => createCompetitionReferee(payload),
    onSuccess: async () => {
      message.success('裁判已新增，账号已按手机号自动创建');
      setModalOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['competition-referees'] });
    },
    onError: (error) => message.error(errorText(error, '裁判新增失败，请刷新后重试')),
  });
  const updateMutation = useMutation({
    mutationFn: (payload: RefereeFormValues) => updateCompetitionReferee(editingId!, payload),
    onSuccess: async () => {
      message.success('裁判信息已更新');
      setModalOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['competition-referees'] });
    },
    onError: (error) => message.error(errorText(error, '裁判更新失败，请刷新后重试')),
  });
  const deleteMutation = useMutation({
    mutationFn: (profileId: CompetitionId) => deleteCompetitionReferee(profileId),
    onSuccess: async () => {
      message.success('裁判已删除');
      await queryClient.invalidateQueries({ queryKey: ['competition-referees'] });
    },
    onError: (error) => message.error(errorText(error, '裁判删除失败，请刷新后重试')),
  });

  const openCreate = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ qualifications: [{} as CompetitionRefereeQualificationRequest] });
    setModalOpen(true);
  };

  const openEdit = (referee: CompetitionReferee) => {
    setEditingId(referee.id);
    form.resetFields();
    setModalOpen(true);
  };

  const closeModal = () => {
    if (createMutation.isPending || updateMutation.isPending) return;
    setModalOpen(false);
    setEditingId(null);
    form.resetFields();
  };

  const submit = async () => {
    const values = await form.validateFields();
    if (editingId === null) createMutation.mutate(values);
    else updateMutation.mutate(values);
  };

  const columns = useMemo<ColumnsType<CompetitionReferee>>(() => [
    { title: '姓名', dataIndex: 'name', width: 110, fixed: 'left' },
    { title: '身份证号', dataIndex: 'idCardNo', width: 180 },
    { title: '手机号', dataIndex: 'mobile', width: 135 },
    {
      title: '相关工种及等级',
      width: 360,
      render: (_, row) => (
        <Space wrap size={[4, 4]}>
          {(row.qualifications ?? []).map((item) => (
            <Tag key={`${idValue(item.workTypeId)}-${item.levelFromCode}-${item.levelToCode}`}>
              {item.workTypeName || idValue(item.workTypeId)} · {item.levelRange
                || `${item.levelFromName || item.levelFromCode}-${item.levelToName || item.levelToCode}`}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: string) => <Tag color={value === 'active' ? 'success' : 'default'}>
        {value === 'active' ? '启用' : value || '-'}
      </Tag>,
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      fixed: 'right',
      render: (_, row) => (
        <Space>
          {canUpdate && <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(row)}>编辑</Button>}
          {row.accountUsername && <Tag color="blue">{row.accountUsername}</Tag>}
          {canDelete && <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            loading={deleteMutation.isPending && deleteMutation.variables === row.id}
            onClick={() => Modal.confirm({
              title: '确认删除该裁判？',
              content: '已绑定选手的裁判不能删除，请先解除所有赛段绑定。',
              okText: '删除',
              cancelText: '取消',
              onOk: () => deleteMutation.mutateAsync(row.id),
            })}
          >删除</Button>}
        </Space>
      ),
    },
  ], [canDelete, canUpdate, deleteMutation]);

  if (!canView) {
    return <Result status="403" title="暂无裁判管理查看权限" subTitle="请联系管理员申请竞赛裁判管理权限。" />;
  }

  return (
    <div data-testid="competition-referee-management-page">
      <Card bordered={false} className="competition-filter-card">
        <div className="competition-filter-card__head">
          <div>
            <Typography.Title level={4}>裁判信息</Typography.Title>
            <Typography.Text type="secondary">
              裁判档案按租户统一维护，可参与多场赛事和多个工种；等级按范围填写，例如“初级工-中级工”或“初级工-高级工”。新增裁判后自动创建手机号登录账号，初始密码为 12345678。分组请到「裁判小组」。
            </Typography.Text>
          </div>
          {canCreate && <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增裁判</Button>}
        </div>
        <Space wrap className="competition-filter-card__body">
          <Select
            allowClear
            placeholder="按工种筛选"
            style={{ minWidth: 220 }}
            value={workTypeId}
            loading={workTypesQuery.isLoading}
            options={workTypeOptions}
            onChange={(value) => {
              setWorkTypeId(value);
              setPage(1);
            }}
          />
          <Input.Search
            allowClear
            placeholder="搜索姓名、身份证号或手机号"
            style={{ width: 260 }}
            onSearch={(value) => {
              setKeyword(value.trim());
              setPage(1);
            }}
            onChange={(event) => {
              if (!event.target.value) {
                setKeyword('');
                setPage(1);
              }
            }}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void refereesQuery.refetch()}>刷新</Button>
        </Space>
      </Card>

      <Card size="small" style={{ marginTop: 16 }}>
        <Table<CompetitionReferee>
          rowKey={(row) => idValue(row.id)}
          scroll={{ x: 1100 }}
          loading={refereesQuery.isLoading || refereesQuery.isFetching}
          dataSource={refereesQuery.data?.items ?? []}
          columns={columns}
          pagination={{
            current: refereesQuery.data?.page ?? page,
            pageSize: refereesQuery.data?.pageSize ?? pageSize,
            total: refereesQuery.data?.total ?? 0,
            showSizeChanger: true,
            onChange: (nextPage, nextPageSize) => {
              setPage(nextPage);
              setPageSize(nextPageSize);
            },
          }}
          locale={{ emptyText: '暂无裁判信息' }}
        />
      </Card>

      <Modal
        title={editingId === null ? '新增裁判' : '编辑裁判'}
        open={modalOpen}
        onCancel={closeModal}
        onOk={() => void submit()}
        confirmLoading={createMutation.isPending || updateMutation.isPending || editQuery.isLoading}
        destroyOnClose
        width={720}
      >
        <Form<RefereeFormValues> form={form} layout="vertical">
          <Form.Item name="name" label="姓名" rules={[{ required: true, whitespace: true, message: '请输入姓名' }]}>
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item
            name="idCardNo"
            label="身份证号"
            rules={[
              { required: true, whitespace: true, message: '请输入身份证号' },
              { validator: (_, value: string) => !value || validIdCard(value) ? Promise.resolve() : Promise.reject(new Error('身份证号格式或校验码不正确')) },
            ]}
          >
            <Input maxLength={18} />
          </Form.Item>
          <Form.Item
            name="mobile"
            label="手机号"
            rules={[
              { required: true, whitespace: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' },
            ]}
          >
            <Input maxLength={11} />
          </Form.Item>
          <Form.List
            name="qualifications"
            rules={[{
              validator: async (_, values: CompetitionRefereeQualificationRequest[]) => {
                if (!values || values.length === 0) throw new Error('至少添加一项工种等级资质');
              },
            }]}
          >
            {(fields, { add, remove }, { errors }) => (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography.Text strong>相关工种与工种等级</Typography.Text>
                  <Button type="dashed" icon={<PlusOutlined />} onClick={() => add()}>
                    添加资质
                  </Button>
                </div>
                {fields.map((field) => {
                  const selectedWorkTypeId = form.getFieldValue(['qualifications', field.name, 'workTypeId']);
                  return (
                    <Space key={field.key} align="baseline" style={{ display: 'flex', marginTop: 12 }}>
                      <Form.Item
                        {...field}
                        name={[field.name, 'workTypeId']}
                        rules={[{ required: true, message: '请选择工种' }]}
                      >
                        <Select
                          showSearch
                          optionFilterProp="label"
                          style={{ width: 330 }}
                          placeholder="选择工种"
                          options={workTypeOptions}
                          onChange={() => {
                            form.setFieldValue(['qualifications', field.name, 'levelFromCode'], undefined);
                            form.setFieldValue(['qualifications', field.name, 'levelToCode'], undefined);
                          }}
                        />
                      </Form.Item>
                      <Form.Item
                        {...field}
                        name={[field.name, 'levelFromCode']}
                        rules={[{ required: true, message: '请选择起始等级' }]}
                      >
                        <Select
                          style={{ width: 135 }}
                          placeholder="起始等级"
                          options={LEVEL_OPTIONS.filter((item) => {
                            const workType = workTypes.find((candidate) => idValue(candidate.id) === idValue(selectedWorkTypeId));
                            const levelsNo = workType?.levelsNo || '';
                            return !levelsNo || levelsNo.includes(item.value);
                          })}
                        />
                      </Form.Item>
                      <Form.Item
                        {...field}
                        name={[field.name, 'levelToCode']}
                        rules={[{ required: true, message: '请选择结束等级' }]}
                      >
                        <Select
                          style={{ width: 135 }}
                          placeholder="结束等级"
                          options={LEVEL_OPTIONS.filter((item) => {
                            const workType = workTypes.find((candidate) => idValue(candidate.id) === idValue(selectedWorkTypeId));
                            const levelsNo = workType?.levelsNo || '';
                            return !levelsNo || levelsNo.includes(item.value);
                          })}
                        />
                      </Form.Item>
                      <Button
                        type="text"
                        danger
                        icon={<MinusCircleOutlined />}
                        disabled={fields.length <= 1}
                        onClick={() => remove(field.name)}
                      />
                    </Space>
                  );
                })}
                <Form.ErrorList errors={errors} />
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  );
}
