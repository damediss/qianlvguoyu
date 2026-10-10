import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ApartmentOutlined,
  BookOutlined,
  DownOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  Tree,
  message,
  Popconfirm,
  Spin,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo } from 'react';
import {
  pageMakeExamPlans,
  createMakeExamPlan,
  updateMakeExamPlan,
  deleteMakeExamPlan,
  pageWorkTypes,
} from '../../api/exam';
import { pageCourses } from '../../api/exam';
import type {
  ExamMakeExamPlan,
  ExamMakeExamPlanCreateReq,
  ExamCourse,
} from '../../types/exam';
import { MakeExamPlanForm, buildMakeExamPlanCreatePayload } from './MakeExamPlanForm';
import './MakeExamPlanPage.css';

const EXAM_TYPE_MAP: Record<number, string> = { 0: '理论', 1: '实操' };

export function MakeExamPlanPage() {
  const queryClient = useQueryClient();
  const [selectedCourseId, setSelectedCourseId] = useState<number | undefined>();
  const [examType, setExamType] = useState<number>(0);
  const [query, setQuery] = useState({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  // 左侧树状态
  const [treeSearchValue, setTreeSearchValue] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);

  // 工种列表(扁平,不分层级)—— /api/exam/work-types/tree 只返回根工种,
  // 会漏掉子工种(如"金属材料热处理工"等 parentId!=0 的节点),
  // 这里用 page 接口拿全部,再按 workTypeId 关联 courses
  const workTypeQuery = useQuery({
    queryKey: ['exam-work-types-all'],
    queryFn: () => pageWorkTypes({ page: 1, pageSize: 500 }),
  });

  // 科目列表
  const courseQuery = useQuery({
    queryKey: ['exam-courses-all'],
    queryFn: () => pageCourses({ page: 1, pageSize: 500 }),
  });

  // 构建"工种 → 科目"树形数据（工种按 sortNumber 升序，科目按 sortNumber 升序）
  const treeData = useMemo(() => {
    // pageWorkTypes 返回的是 PageResponse,需要从 .items 拿
    const workTypes = ((workTypeQuery.data as any)?.items || [])
      .slice()
      .sort((a: any, b: any) => (a.sortNumber ?? 0) - (b.sortNumber ?? 0));
    const courses = courseQuery.data?.items || [];

    // 按 workTypeId 分组科目(容错: workTypeId 可能是 string 也可能是 number)
    const coursesByWorkType = new Map<number, ExamCourse[]>();
    courses.forEach((c: ExamCourse) => {
      const wtId = c.workTypeId ?? 0;
      if (!coursesByWorkType.has(wtId)) coursesByWorkType.set(wtId, []);
      coursesByWorkType.get(wtId)!.push(c);
    });
    // 科目按 sortNumber 升序
    coursesByWorkType.forEach((list) => {
      list.sort((a, b) => (a.sortNumber ?? 0) - (b.sortNumber ?? 0));
    });

    const search = treeSearchValue.trim().toLowerCase();

    return workTypes
      .map((wt: any) => {
        const wtCourses = coursesByWorkType.get(wt.id) || [];
        const filteredCourses = search
          ? wtCourses.filter((c) => c.title.toLowerCase().includes(search))
          : wtCourses;
        // 工种本身匹配搜索词时显示全部科目，否则只显示匹配的科目
        const wtMatches = search ? wt.title.toLowerCase().includes(search) : false;
        const coursesToShow = wtMatches ? wtCourses : filteredCourses;

        if (search && !wtMatches && coursesToShow.length === 0) return null;

        return {
          key: `workType-${wt.id}`,
          title: (
            <span
              style={{
                fontWeight: 600,
                fontSize: 13,
                color: 'rgba(0, 0, 0, 0.88)',
              }}
            >
              {wt.title}
              <span style={{ color: 'rgba(0, 0, 0, 0.45)', fontWeight: 400, marginLeft: 6, fontSize: 12 }}>
                ({wtCourses.length})
              </span>
            </span>
          ),
          icon: <ApartmentOutlined style={{ color: '#1677ff' }} />,
          selectable: false,
          children: coursesToShow.map((c) => {
            const isSelected = selectedKeys.includes(`course-${c.id}`);
            return {
              key: `course-${c.id}`,
              title: (
                <span
                  style={{
                    color: isSelected ? '#fff' : 'rgba(0, 0, 0, 0.88)',
                    fontWeight: isSelected ? 500 : 400,
                    display: 'inline-block',
                    width: '100%',
                  }}
                >
                  {c.title}
                </span>
              ),
              icon: (
                <BookOutlined
                  style={{ color: isSelected ? '#fff' : '#1677ff' }}
                />
              ),
              isLeaf: true,
              style: isSelected
                ? {
                    backgroundColor: '#1677ff',
                    borderRadius: 6,
                    color: '#fff',
                    margin: '2px 4px',
                  }
                : {
                    borderRadius: 6,
                    margin: '2px 4px',
                    transition: 'background-color 0.15s',
                  },
            };
          }),
        };
      })
      .filter(Boolean) as any[];
  }, [workTypeQuery.data, courseQuery.data, treeSearchValue, selectedKeys]);

  // 默认展开所有工种节点
  const defaultExpandAll = useMemo(() => {
    const workTypes = ((workTypeQuery.data as any)?.items || []) as any[];
    const keys = workTypes.map((wt) => `workType-${wt.id}`);
    return keys;
  }, [workTypeQuery.data]);

  // 首次加载时展开所有工种
  useMemo(() => {
    if (defaultExpandAll.length > 0 && expandedKeys.length === 0) {
      setExpandedKeys(defaultExpandAll);
    }
  }, [defaultExpandAll, expandedKeys.length]);

  // 组卷计划分页
  const listQuery = useQuery({
    queryKey: ['make-exam-plans', { ...query, courseId: selectedCourseId, examType }],
    queryFn: () =>
      pageMakeExamPlans({ ...query, courseId: selectedCourseId, examType }),
  });

  const createMutation = useMutation({
    mutationFn: createMakeExamPlan,
    onSuccess: () => {
      message.success('组卷计划已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['make-exam-plans'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamMakeExamPlanCreateReq }) =>
      updateMakeExamPlan(id, payload),
    onSuccess: () => {
      message.success('组卷计划已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['make-exam-plans'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteMakeExamPlan,
    onSuccess: () => {
      message.success('组卷计划已删除');
      queryClient.invalidateQueries({ queryKey: ['make-exam-plans'] });
    },
  });

  const handleAdd = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ examType, courseId: selectedCourseId, isAll: 0, isComputer: 1, isPrint: 0 });
    setModalOpen(true);
  }, [form, examType, selectedCourseId]);

  const handleEdit = useCallback(
    (record: ExamMakeExamPlan) => {
      setEditingId(record.id);
      form.setFieldsValue({
        ...record,
        isComputer: record.isComputer === 1,
        isPrint: record.isPrint === 1,
        detailList: record.planDetailList?.map((d) => ({
          type: d.type,
          num: d.num,
          point: d.point,
          startPoint: d.startPoint,
          endPoint: d.endPoint,
          difficultyStart: d.difficultyStart,
          difficultyEnd: d.difficultyEnd,
        })),
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const payload = buildMakeExamPlanCreatePayload(values);
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    } catch {
      // validation error
    }
  }, [form, editingId, createMutation, updateMutation]);

  const columns: ColumnsType<ExamMakeExamPlan> = [
    { title: '序号', width: 45, render: (_, __, i) => i + 1 },
    { title: '计划名称', dataIndex: 'title', width: 220, ellipsis: true },
    { title: '试卷类型', dataIndex: 'examType', width: 60, render: (v: number) => EXAM_TYPE_MAP[v] || v },
    { title: '考试时长(分钟)', dataIndex: 'time', width: 60 },
    {
      title: '机考', dataIndex: 'isComputer', width: 50,
      render: (v: number) => (v === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '打印', dataIndex: 'isPrint', width: 50,
      render: (v: number) => (v === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    { title: '总分', dataIndex: 'totalPoint', width: 55 },
    { title: '及格分', dataIndex: 'passPoint', width: 50 },
    { title: '备注', dataIndex: 'comments', width: 75, ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', width: 120 },
    {
      title: '操作', width: 130, fixed: 'right',
      render: (_, record) => (
        <Space size={0}>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMutation.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const courses = courseQuery.data?.items || [];

  const handleTreeSelect = useCallback(
    (keys: React.Key[]) => {
      setSelectedKeys(keys);
      if (keys.length === 0) {
        setSelectedCourseId(undefined);
      } else {
        const key = String(keys[0]);
        if (key.startsWith('course-')) {
          setSelectedCourseId(Number(key.replace('course-', '')));
        } else {
          setSelectedCourseId(undefined);
        }
      }
      setQuery((q) => ({ ...q, page: 1 }));
    },
    [],
  );

  return (
    <div style={{ display: 'flex', gap: 16, height: '100%' }}>
      {/* 左侧：工种-科目 树形筛选 */}
      <Card
        style={{ width: 320, flexShrink: 0, display: 'flex', flexDirection: 'column' }}
        title="工种-科目筛选"
        size="small"
        bodyStyle={{ flex: 1, overflow: 'auto', padding: '8px 0' }}
      >
        <Input
          placeholder="搜索工种/科目"
          prefix={<SearchOutlined />}
          allowClear
          style={{ marginBottom: 8, margin: '0 8px 8px' }}
          value={treeSearchValue}
          onChange={(e) => setTreeSearchValue(e.target.value)}
        />
        {workTypeQuery.isLoading ? (
          <div style={{ textAlign: 'center', padding: 24 }}><Spin size="small" /></div>
        ) : (
          <Tree
            treeData={treeData}
            expandedKeys={expandedKeys}
            selectedKeys={selectedKeys}
            onExpand={(keys) => setExpandedKeys(keys)}
            onSelect={handleTreeSelect}
            showIcon
            showLine
            blockNode
            switcherIcon={<DownOutlined />}
            style={{ padding: '0 4px' }}
            className="work-type-tree"
          />
        )}
        {selectedCourseId && (
          <Button
            type="link"
            size="small"
            onClick={() => {
              setSelectedCourseId(undefined);
              setSelectedKeys([]);
            }}
            style={{ margin: '8px 8px 0' }}
          >
            清除筛选
          </Button>
        )}
      </Card>

      {/* 右侧主区域 */}
      <div style={{ flex: 1 }}>
        <Tabs
          activeKey={String(examType)}
          onChange={(k) => {
            setExamType(Number(k));
            setQuery({ ...query, page: 1 });
          }}
          items={[
            { key: '0', label: '理论组卷' },
            { key: '1', label: '实操组卷' },
          ]}
          style={{ marginBottom: 0 }}
        />
        <Space style={{ marginBottom: 12 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => listQuery.refetch()}
          >
            刷新
          </Button>
        </Space>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={listQuery.data?.items || []}
          loading={listQuery.isLoading}
          scroll={{ x: 1000 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: listQuery.data?.total || 0,
            showSizeChanger: true,
            onChange: (p, ps) => setQuery({ page: p, pageSize: ps }),
          }}
        />
      </div>

      {/* 弹窗表单：与跟班「添加组卷」共用 MakeExamPlanForm */}
      <Modal
        title={editingId ? '编辑组卷计划' : '添加组卷计划'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        width={900}
        destroyOnHidden
      >
        <MakeExamPlanForm form={form} courses={courses} />
      </Modal>
    </div>
  );
}
