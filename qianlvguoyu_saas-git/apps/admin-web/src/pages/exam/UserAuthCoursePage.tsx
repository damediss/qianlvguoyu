import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyOutlined,
  SearchOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  pageUserAuthCourses,
  getUserAuthCourse,
  createUserAuthCourse,
  updateUserAuthCourse,
  deleteUserAuthCourse,
  pageCourses,
} from '../../api/exam';
import type {
  ExamUserAuthCourse,
  ExamUserAuthCourseCreateReq,
  ExamUserAuthCourseItem,
  ExamCourse,
} from '../../types/exam';

const TYPE_OPTIONS = [
  { label: '命题科目', value: 0 },
  { label: '审题科目', value: 1 },
  { label: '终审科目', value: 2 },
];

const TYPE_MAP: Record<number, string> = {
  0: '命题',
  1: '审题',
  2: '终审',
};

export function UserAuthCoursePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<{ realname: string; idCard: string } | null>(null);
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['exam-user-auth-courses', query],
    queryFn: () => pageUserAuthCourses(query),
  });

  const coursesQuery = useQuery({
    queryKey: ['exam-courses-all'],
    queryFn: () => pageCourses({ page: 1, pageSize: 200 }),
    staleTime: 60000,
  });

  const createMutation = useMutation({
    mutationFn: createUserAuthCourse,
    onSuccess: () => {
      message.success('专家授权已创建');
      setModalOpen(false);
      setEditingKey(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      oldRealname,
      oldIdCard,
      payload,
    }: {
      oldRealname: string;
      oldIdCard: string;
      payload: ExamUserAuthCourseCreateReq;
    }) => updateUserAuthCourse(oldRealname, oldIdCard, payload),
    onSuccess: () => {
      message.success('专家授权已更新');
      setModalOpen(false);
      setEditingKey(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ realname, idCard }: { realname: string; idCard: string }) =>
      deleteUserAuthCourse(realname, idCard),
    onSuccess: () => {
      message.success('专家授权已删除');
      queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
    },
  });

  const openCreate = useCallback(() => {
    setEditingKey(null);
    form.resetFields();
    form.setFieldsValue({ status: true, sortNumber: 1, items: [] });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    async (record: ExamUserAuthCourse) => {
      setEditingKey({ realname: record.realname, idCard: record.idCard });
      form.setFieldsValue({
        realname: record.realname,
        idCard: record.idCard,
        status: record.status === 1,
        sortNumber: record.sortNumber,
        items: record.courses.map((c) => ({
          courseId: c.courseId,
          type: c.type,
        })),
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(() => {
    form.validateFields().then((values) => {
      const courseList: ExamCourse[] = coursesQuery.data?.items || [];
      const items: ExamUserAuthCourseItem[] = (values.items || []).map(
        (item: { courseId: number; type: number }) => {
          const course = courseList.find((c) => c.id === item.courseId);
          return {
            courseId: item.courseId,
            courseTitle: course?.title || '',
            type: item.type ?? 0,
          };
        },
      );

      const payload: ExamUserAuthCourseCreateReq = {
        realname: values.realname,
        idCard: values.idCard || '',
        items,
        status: values.status ? 1 : 0,
        sortNumber: values.sortNumber ?? 1,
      };

      if (editingKey) {
        updateMutation.mutate({
          oldRealname: editingKey.realname,
          oldIdCard: editingKey.idCard,
          payload,
        });
      } else {
        createMutation.mutate(payload);
      }
    });
  }, [form, editingKey, createMutation, updateMutation, coursesQuery.data]);

  const handleDelete = useCallback(
    (record: ExamUserAuthCourse) => {
      Modal.confirm({
        title: `确认删除「${record.realname}」的授权？`,
        content: '将删除该专家的所有科目授权记录。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () =>
          deleteMutation.mutate({ realname: record.realname, idCard: record.idCard }),
      });
    },
    [deleteMutation],
  );

  const courseOptions = (coursesQuery.data?.items || []).map((c) => ({
    label: c.title,
    value: c.id,
  }));

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;

  const columns = [
    {
      title: '专家姓名',
      dataIndex: 'realname',
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    { title: '身份证号', dataIndex: 'idCard', width: 180 },
    {
      title: '授权科目',
      dataIndex: 'courses',
      width: 360,
      render: (courses: ExamUserAuthCourseItem[]) => {
        if (!courses || courses.length === 0) return <Tag>无</Tag>;
        return (
          <Space size={4} wrap>
            {courses.map((c, i) => (
              <Tag key={i} color="blue">
                {c.courseTitle}-{TYPE_MAP[c.type] || c.type}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: number) =>
        v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    { title: '排序号', dataIndex: 'sortNumber', width: 80 },
    {
      title: '操作',
      width: 120,
      render: (_: any, record: ExamUserAuthCourse) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><SafetyOutlined /></span>
            <Typography.Title level={1}>专家授权管理</Typography.Title>
          </div>
          <Typography.Text>
            管理专家与科目的授权关系，支持命题、审题、终审三种类型。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
            }}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增授权
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索专家姓名、身份证号、科目"
            allowClear
            style={{ width: 300 }}
            value={query.keyword}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))
            }
            onPressEnter={() =>
              queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] })
            }
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() =>
              queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] })
            }
          >
            搜索
          </Button>
        </Space>
        <Table
          rowKey={(r: ExamUserAuthCourse) => `${r.realname}::${r.idCard}`}
          size="middle"
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, pageSize) =>
              setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <Modal
        title={editingKey ? '编辑专家授权' : '新增专家授权'}
        open={modalOpen}
        width={640}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingKey(null);
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Space size={12} style={{ width: '100%' }}>
            <Form.Item
              name="realname"
              label="专家姓名"
              rules={[{ required: true, message: '请输入专家姓名' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="请输入专家姓名" maxLength={255} />
            </Form.Item>
            <Form.Item
              name="idCard"
              label="身份证号"
              style={{ flex: 1 }}
            >
              <Input placeholder="请输入身份证号" maxLength={50} />
            </Form.Item>
          </Space>

          <Form.List name="items">
            {(fields, { add, remove }) => (
              <>
                <div style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Typography.Text strong>授权科目</Typography.Text>
                  <Button
                    type="dashed"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() => add({ courseId: undefined, type: 0 })}
                  >
                    添加科目
                  </Button>
                </div>
                {fields.map(({ key, name, ...rest }) => (
                  <Space
                    key={key}
                    size={12}
                    style={{ display: 'flex', marginBottom: 8 }}
                    align="baseline"
                  >
                    <Form.Item
                      {...rest}
                      name={[name, 'courseId']}
                      rules={[{ required: true, message: '请选择科目' }]}
                      style={{ width: 280 }}
                    >
                      <Select
                        showSearch
                        placeholder="选择科目"
                        options={courseOptions}
                        filterOption={(input, option) =>
                          (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
                        }
                      />
                    </Form.Item>
                    <Form.Item
                      {...rest}
                      name={[name, 'type']}
                      rules={[{ required: true, message: '请选择类型' }]}
                      style={{ width: 160 }}
                    >
                      <Select placeholder="类型" options={TYPE_OPTIONS} />
                    </Form.Item>
                    <Button
                      type="text"
                      danger
                      icon={<MinusCircleOutlined />}
                      onClick={() => remove(name)}
                    />
                  </Space>
                ))}
                {fields.length === 0 && (
                  <Typography.Text type="secondary">
                    请至少添加一个授权科目
                  </Typography.Text>
                )}
              </>
            )}
          </Form.List>

          <Space size={12}>
            <Form.Item name="status" label="状态" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Space>
          <Form.Item name="sortNumber" label="排序号">
            <InputNumber min={1} max={9999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}