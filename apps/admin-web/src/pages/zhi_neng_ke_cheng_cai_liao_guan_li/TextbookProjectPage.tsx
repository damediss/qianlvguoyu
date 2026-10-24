/**
 * 教材工程列表页 + 新建向导 —— 《职业标准驱动教材生成·一期实施设计》v1.0 §3.2
 * 菜单：智能课程材料管理 > 教材生成管理（code textbook_project）
 * 大纲编辑页为隐藏路由（/textbook/outline/:projectId），按 pushState + popstate 约定跳转
 */
import { useState } from 'react';
import {
  BookOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  InboxOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  createProject,
  deleteProject,
  exportProject,
  pageProjects,
  parsePreview,
  updateProject,
} from '../../api/textbook';
import { useAuthStore } from '../../stores/authStore';
import { parseContentDispositionFilename } from '../../utils/download';
import {
  TEXTBOOK_LEVEL_OPTIONS,
  textbookLevelLabel,
  type ParsePreviewVO,
  type ProjectUpdateReq,
  type TextbookProjectStatus,
  type TextbookProjectVO,
} from '../../types/textbook';

const STATUS_META: Record<TextbookProjectStatus, { text: string; color: string }> = {
  parsed: { text: '已解析', color: 'default' },
  outlined: { text: '大纲已生成', color: 'blue' },
  reviewed: { text: '评审通过', color: 'green' },
};

const STATUS_OPTIONS = (Object.keys(STATUS_META) as TextbookProjectStatus[]).map((s) => ({
  value: s,
  label: STATUS_META[s].text,
}));

/** 隐藏路由跳转（与 main.tsx 约定：pushState + popstate） */
function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

function errorText(e: unknown, fallback: string): string {
  const err = e as { response?: { data?: { message?: string } }; message?: string };
  return err?.response?.data?.message || err?.message || fallback;
}

export function TextbookProjectPage() {
  const queryClient = useQueryClient();
  const hasPermission = useAuthStore((s) => s.hasPermission);
  const canManage = hasPermission('textbook:project:manage');
  const canExport = hasPermission('textbook:project:export');

  const [query, setQuery] = useState<{
    page: number;
    pageSize: number;
    workTypeTitle?: string;
    direction?: string;
    levelCode?: number;
    status?: TextbookProjectStatus;
  }>({ page: 1, pageSize: 20 });
  const [wizardOpen, setWizardOpen] = useState(false);
  const [editing, setEditing] = useState<TextbookProjectVO | null>(null);
  const [editForm] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['textbook-projects', query],
    queryFn: () => pageProjects(query),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, req }: { id: string; req: ProjectUpdateReq }) => updateProject(id, req),
    onSuccess: () => {
      message.success('已保存');
      setEditing(null);
      queryClient.invalidateQueries({ queryKey: ['textbook-projects'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: () => {
      message.success('已删除（含大纲节点）');
      queryClient.invalidateQueries({ queryKey: ['textbook-projects'] });
    },
  });

  const handleExport = async (record: TextbookProjectVO, format: 'md' | 'xlsx') => {
    try {
      const resp = await exportProject(record.id, format);
      const blob = resp.data as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download =
        parseContentDispositionFilename(resp) ??
        `教材目录_${record.workTypeTitle}_${record.direction}_${textbookLevelLabel(record.levelCode)}.${format === 'md' ? 'md' : 'xlsx'}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (e) {
      message.error(errorText(e, '导出失败'));
    }
  };

  const columns: ColumnsType<TextbookProjectVO> = [
    { title: '工种', dataIndex: 'workTypeTitle', width: 160 },
    { title: '方向', dataIndex: 'direction', width: 120 },
    {
      title: '等级',
      dataIndex: 'levelCode',
      width: 120,
      render: (v: number) => textbookLevelLabel(v),
    },
    { title: '学时', dataIndex: 'trainingHours', width: 80 },
    { title: '总字数', dataIndex: 'totalWords', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (s: TextbookProjectStatus) => (
        <Tag color={STATUS_META[s]?.color}>{STATUS_META[s]?.text || s}</Tag>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 140,
      render: (v: string | null) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '操作',
      width: 260,
      fixed: 'right',
      render: (_, record) => (
        <Space size={0}>
          <Button size="small" type="link" onClick={() => navigateTo(`/textbook/outline/${record.id}`)}>
            大纲
          </Button>
          {canExport && (
            <Dropdown
              menu={{
                items: [
                  { key: 'md', label: 'Markdown' },
                  { key: 'xlsx', label: 'Excel' },
                ],
                onClick: ({ key }) => handleExport(record, key as 'md' | 'xlsx'),
              }}
            >
              <Button size="small" type="link">
                导出 <DownOutlined />
              </Button>
            </Dropdown>
          )}
          {canManage && (
            <Button
              size="small"
              type="link"
              icon={<EditOutlined />}
              onClick={() => {
                setEditing(record);
                editForm.setFieldsValue({
                  workTypeTitle: record.workTypeTitle,
                  trainingHours: record.trainingHours,
                  wordsPerHour: record.wordsPerHour,
                });
              }}
            >
              编辑
            </Button>
          )}
          {canManage && (
            <Popconfirm
              title={`确认删除「${record.workTypeTitle}·${record.direction}·${textbookLevelLabel(record.levelCode)}」？`}
              description="将同时删除该工程下的大纲节点。"
              onConfirm={() => deleteMutation.mutate(record.id)}
            >
              <Button size="small" type="link" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card
      title={
        <Space>
          <BookOutlined />
          <span>教材生成管理（职业标准 → 教材大纲）</span>
        </Space>
      }
      extra={
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['textbook-projects'] })}
          >
            刷新
          </Button>
          {canManage && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setWizardOpen(true)}>
              新建教材工程
            </Button>
          )}
        </Space>
      }
    >
      {canManage ? null : (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="当前账号仅有查看权限，无法新建、编辑或删除教材工程。"
        />
      )}
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          placeholder="工种名称"
          allowClear
          style={{ width: 180 }}
          value={query.workTypeTitle ?? ''}
          onChange={(e) =>
            setQuery((prev) => ({ ...prev, workTypeTitle: e.target.value || undefined, page: 1 }))
          }
        />
        <Input
          placeholder="方向"
          allowClear
          style={{ width: 140 }}
          value={query.direction ?? ''}
          onChange={(e) =>
            setQuery((prev) => ({ ...prev, direction: e.target.value || undefined, page: 1 }))
          }
        />
        <Select
          placeholder="等级"
          allowClear
          style={{ width: 140 }}
          value={query.levelCode}
          options={TEXTBOOK_LEVEL_OPTIONS}
          onChange={(v) => setQuery((prev) => ({ ...prev, levelCode: v, page: 1 }))}
        />
        <Select
          placeholder="状态"
          allowClear
          style={{ width: 130 }}
          value={query.status}
          options={STATUS_OPTIONS}
          onChange={(v) => setQuery((prev) => ({ ...prev, status: v, page: 1 }))}
        />
      </Space>
      <Table<TextbookProjectVO>
        rowKey="id"
        size="middle"
        loading={listQuery.isLoading}
        dataSource={listQuery.data?.items || []}
        columns={columns}
        scroll={{ x: 1100 }}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total: listQuery.data?.total || 0,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
        }}
      />

      {wizardOpen && (
        <CreateProjectWizard
          onClose={() => setWizardOpen(false)}
          onCreated={(project) => {
            setWizardOpen(false);
            queryClient.invalidateQueries({ queryKey: ['textbook-projects'] });
            navigateTo(`/textbook/outline/${project.id}`);
          }}
        />
      )}

      <Modal
        title="编辑教材工程"
        open={!!editing}
        width={480}
        confirmLoading={updateMutation.isPending}
        onCancel={() => setEditing(null)}
        onOk={() =>
          editForm.validateFields().then((values) => {
            if (!editing) return;
            updateMutation.mutate({
              id: editing.id,
              req: {
                workTypeTitle: values.workTypeTitle,
                trainingHours: values.trainingHours,
                wordsPerHour: values.wordsPerHour,
              },
            });
          })
        }
        destroyOnHidden
      >
        <Form form={editForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="workTypeTitle"
            label="职业名称"
            rules={[{ required: true, message: '请输入职业名称' }]}
          >
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item name="trainingHours" label="培训参考学时" tooltip={HOURS_TOOLTIP}>
            <InputNumber min={1} max={100000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="wordsPerHour"
            label="每学时字数系数"
            tooltip={`${WORDS_PER_HOUR_TOOLTIP} 修改学时或系数后将自动重建大纲骨架并重新配比。`}
          >
            <InputNumber min={100} max={100000} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}

// ==================================================================
// 新建向导：上传 docx 解析预览 → 选方向×等级×职业功能 → 确认创建
// ==================================================================

/** 解析不出方向映射时，职业功能勾选的兜底选项范围（1.4 节职业功能序号） */
const FALLBACK_FUNCTION_NOS = Array.from({ length: 12 }, (_, i) => i + 1);

/** 字段名词解释（问号气泡，向导与编辑弹窗共用） */
const HOURS_TOOLTIP =
  '本等级在国家职业技能标准中规定的培训参考学时，上传标准解析成功后会自动回填，一般无需手改。'
  + '总字数 = 培训参考学时 × 每学时字数系数；标准未提供时，请与教务确认后填写。';
const WORDS_PER_HOUR_TOOLTIP =
  '每 1 学时对应的目标字数，用来估算教材总篇幅：总字数 = 培训参考学时 × 每学时字数系数。'
  + '默认 2500，即 40 学时约生成 10 万字的教材；无特殊篇幅要求建议保持默认。';
const FUNCTION_TOOLTIP =
  '国家职业技能标准「工作要求」表的第一级分类（如 工作准备、铝电解槽操作、培训与管理）。'
  + '勾选的功能将作为教材大纲专业篇的章生成内容；已按所选方向自动预勾选，可按需调整。';

function CreateProjectWizard({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (project: TextbookProjectVO) => void;
}) {
  const [step, setStep] = useState(0);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ParsePreviewVO | null>(null);
  const [parseError, setParseError] = useState<string | null>(null);
  const [parsing, setParsing] = useState(false);
  const [form] = Form.useForm();
  // 离开 step1 时把校验后的表单值存本地：Form 在切到 step2 后卸载，getFieldsValue 不再返回保留值
  const [step1Values, setStep1Values] = useState<{
    direction: string;
    levelCode: number;
    functionNos: number[];
    trainingHours?: number;
    wordsPerHour?: number;
  } | null>(null);

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: (project) => {
      message.success('教材工程已创建');
      onCreated(project);
    },
  });

  const handleUpload = async (uploadFile: File) => {
    setFile(uploadFile);
    setParsing(true);
    setPreview(null);
    setParseError(null);
    try {
      const data = await parsePreview(uploadFile);
      setPreview(data);
    } catch (e) {
      setParseError(errorText(e, '解析失败'));
    } finally {
      setParsing(false);
    }
  };

  const directions = preview?.directions ?? [];

  const applyDirection = (dir: string) => {
    const levels =
      preview?.directionLevels?.[dir]?.length ? preview.directionLevels[dir] : TEXTBOOK_LEVEL_OPTIONS.map((o) => o.value);
    const first = levels[0];
    const mapped = mappedFunctionNos(first, dir);
    form.setFieldsValue({
      direction: dir,
      levelCode: first,
      trainingHours: hoursOf(first) ?? form.getFieldValue('trainingHours'),
      ...(mapped ? { functionNos: mapped } : {}),
    });
  };

  const hoursOf = (levelCode: number | undefined): number | undefined => {
    if (levelCode == null || !preview) return undefined;
    return preview.trainingHoursByLevel?.[levelCode] ?? preview.trainingHoursByLevel?.[String(levelCode)];
  };

  /** 所选等级的职业功能序号→名称（后端解析工作要求表 L1；无名称时 undefined） */
  const functionNamesOf = (levelCode: number | undefined): Record<string, string> | undefined => {
    if (levelCode == null || !preview) return undefined;
    return preview.functionNamesByLevel?.[levelCode] ?? preview.functionNamesByLevel?.[String(levelCode)];
  };

  /** (等级, 方向) 对应的解析映射序号（勾选预选随选择联动；无映射返回 undefined 不覆盖手工勾选） */
  const mappedFunctionNos = (levelCode: number | undefined, direction: string | undefined): number[] | undefined => {
    if (levelCode == null || !direction || !preview) return undefined;
    const byLevel =
      preview.guessedFunctionNosByLevel?.[levelCode] ?? preview.guessedFunctionNosByLevel?.[String(levelCode)];
    return byLevel?.[direction];
  };

  const selectedDirection = Form.useWatch('direction', form) as string | undefined;
  const selectedLevel = Form.useWatch('levelCode', form) as number | undefined;
  const allowedLevels = selectedDirection
    ? preview?.directionLevels?.[selectedDirection]?.length
      ? preview.directionLevels[selectedDirection]
      : TEXTBOOK_LEVEL_OPTIONS.map((o) => o.value)
    : [];
  const levelOptions = TEXTBOOK_LEVEL_OPTIONS.map((o) => ({
    ...o,
    disabled: allowedLevels.length > 0 && !allowedLevels.includes(o.value),
  }));

  const levelFunctionNames = functionNamesOf(selectedLevel);
  const functionOptions =
    levelFunctionNames && Object.keys(levelFunctionNames).length > 0
      ? Object.entries(levelFunctionNames).map(([no, name]) => ({ value: Number(no), label: `${no} ${name}` }))
      : (preview?.guessedFunctionNos?.length ? preview.guessedFunctionNos : FALLBACK_FUNCTION_NOS).map((n) => ({
          value: n,
          label: `职业功能 ${n}`,
        }));
  const confirmNameOf = (no: number): string => {
    const name = functionNamesOf(step1Values?.levelCode)?.[no];
    return name ? `${no} ${name}` : `职业功能 ${no}`;
  };

  const enterStep = (next: number) => {
    if (next === 1 && preview && !form.getFieldValue('direction')) {
      form.setFieldsValue({
        functionNos: preview.guessedFunctionNos,
        wordsPerHour: 2500,
      });
      if (preview.directions.length === 1) {
        applyDirection(preview.directions[0]);
      }
    }
    setStep(next);
  };

  const submit = () => {
    if (!file || !step1Values) {
      message.warning('请先完成前两个步骤');
      return;
    }
    createMutation.mutate({
      file,
      direction: step1Values.direction,
      levelCode: step1Values.levelCode,
      functionNos: step1Values.functionNos,
      trainingHours: step1Values.trainingHours,
      wordsPerHour: step1Values.wordsPerHour,
    });
  };

  return (
    <Modal
      title="新建教材工程"
      open
      width={720}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>
          取消
        </Button>,
        step > 0 && (
          <Button key="prev" onClick={() => setStep(step - 1)}>
            上一步
          </Button>
        ),
        step < 2 && (
          <Button
            key="next"
            type="primary"
            disabled={step === 0 && (!preview || !!parseError)}
            onClick={() => {
              if (step === 1) {
                form.validateFields().then((values) => {
                  setStep1Values(values as typeof step1Values);
                  setStep(2);
                });
              } else {
                enterStep(1);
              }
            }}
          >
            下一步
          </Button>
        ),
        step === 2 && (
          <Button
            key="submit"
            type="primary"
            loading={createMutation.isPending}
            onClick={submit}
          >
            {createMutation.isPending ? '解析并生成大纲中…' : '创建并生成大纲'}
          </Button>
        ),
      ]}
      destroyOnHidden
    >
      <Steps
        current={step}
        size="small"
        style={{ marginBottom: 24, marginTop: 8 }}
        items={[{ title: '上传标准 docx' }, { title: '方向 × 等级' }, { title: '确认创建' }]}
      />

      {step === 0 && (
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Upload.Dragger
            accept=".docx"
            showUploadList={false}
            disabled={parsing}
            customRequest={({ file: f }) => handleUpload(f as File)}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽上传国家职业技能标准 docx</p>
            <p className="ant-upload-hint">
              {parsing ? '解析中…' : '上传后同步解析预览（不落库），可直接进入下一步'}
            </p>
          </Upload.Dragger>
          {parseError && <Alert type="error" showIcon message="解析失败" description={parseError} />}
          {preview && !parseError && (
            <>
              <Descriptions
                bordered
                size="small"
                column={2}
                items={[
                  { key: '1', label: '职业名称', children: preview.workTypeTitle },
                  { key: '2', label: '职业编码', children: preview.seriesNo || '-' },
                  { key: '3', label: '标准版本', children: preview.standardVersion || '-' },
                  {
                    key: '4',
                    label: '方向',
                    children:
                      preview.directions.length === 1 && preview.directions[0] === '默认'
                        ? '（不分工种）'
                        : preview.directions.join('、'),
                  },
                ]}
              />
              {preview.warnings?.length > 0 && (
                <Alert
                  type="warning"
                  showIcon
                  message={`解析警告 ${preview.warnings.length} 条`}
                  description={preview.warnings.map((w, i) => (
                    <div key={i}>{w}</div>
                  ))}
                />
              )}
            </>
          )}
        </Space>
      )}

      {step === 1 && preview && (
        <Form form={form} layout="vertical" initialValues={step1Values ?? undefined}>
          {preview.guessedFunctionNos.length === 0 && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message="未识别方向映射，请人工勾选职业功能"
            />
          )}
          <Form.Item
            name="direction"
            label="方向"
            rules={[{ required: true, message: '请选择方向' }]}
          >
            <Select
              placeholder="请选择方向"
              options={directions.map((d) => ({ value: d, label: d === '默认' ? '（不分工种）' : d }))}
              onChange={applyDirection}
            />
          </Form.Item>
          <Form.Item
            name="levelCode"
            label="等级"
            rules={[{ required: true, message: '请选择等级' }]}
          >
            <Select
              placeholder="请选择等级"
              options={levelOptions}
              onChange={(v: number) => {
                const patch: { trainingHours?: number; functionNos?: number[] } = {};
                const hours = hoursOf(v);
                if (hours != null) patch.trainingHours = hours;
                const mapped = mappedFunctionNos(v, form.getFieldValue('direction'));
                if (mapped) patch.functionNos = mapped;
                if (Object.keys(patch).length > 0) form.setFieldsValue(patch);
              }}
            />
          </Form.Item>
          <Form.Item
            name="functionNos"
            label="职业功能（勾选生成分册大纲的章）"
            tooltip={FUNCTION_TOOLTIP}
            rules={[{ required: true, message: '请至少勾选一个职业功能' }]}
          >
            <Select mode="multiple" placeholder="请选择职业功能" options={functionOptions} />
          </Form.Item>
          <Space size={16}>
            <Form.Item
              name="trainingHours"
              label="培训参考学时"
              tooltip={HOURS_TOOLTIP}
              rules={[{ required: true, message: '请填写学时' }]}
            >
              <InputNumber min={1} max={100000} style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="wordsPerHour" label="每学时字数系数" tooltip={WORDS_PER_HOUR_TOOLTIP}>
              <InputNumber min={100} max={100000} style={{ width: 180 }} />
            </Form.Item>
          </Space>
        </Form>
      )}

      {step === 2 && step1Values && (
        <ConfirmPanel values={step1Values} file={file} nameOf={confirmNameOf} />
      )}
    </Modal>
  );
}

function ConfirmPanel({
  values,
  file,
  nameOf,
}: {
  values: {
    direction: string;
    levelCode: number;
    functionNos: number[];
    trainingHours?: number;
    wordsPerHour?: number;
  };
  file: File | null;
  nameOf: (no: number) => string;
}) {
  const hours = values.trainingHours ?? 0;
  const perHour = values.wordsPerHour ?? 2500;
  return (
    <Descriptions
      bordered
      column={1}
      items={[
        { key: 'file', label: '标准文档', children: file?.name || '-' },
        { key: 'direction', label: '方向', children: values.direction || '-' },
        { key: 'level', label: '等级', children: textbookLevelLabel(values.levelCode) },
        {
          key: 'functions',
          label: '职业功能',
          children: (values.functionNos || []).map((n) => nameOf(n)).join('、') || '-',
        },
        { key: 'hours', label: '培训参考学时', children: hours },
        { key: 'words', label: '每学时字数系数', children: perHour },
        { key: 'total', label: '预计总字数', children: hours * perHour },
      ]}
    />
  );
}
