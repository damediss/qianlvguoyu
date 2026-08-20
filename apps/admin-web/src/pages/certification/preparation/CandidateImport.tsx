import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  FileProtectOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Popconfirm,
  Segmented,
  Select,
  Space,
  Table,
  Typography,
  Upload,
  message,
} from 'antd';
import type { UploadFile } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { useMemo, useState, type Key } from 'react';
import { pagePeople } from '../../../api/training';
import { getCertificationTask } from '../../../api/certification';
import {
  createPreparationCandidate,
  deletePreparationCandidate,
  getPreparationCandidateApplicationSource,
  importPreparationCandidates,
  importPreparationCandidatesExcel,
  downloadPreparationCandidateExcelTemplate,
  generatePreparationImportBatchMaterials,
  listAllPreparationCandidates,
  pageCandidateStaging,
  pagePreparationCandidates,
  pagePreparationImportErrors,
  promoteCandidateStaging,
  updatePreparationCandidate,
} from '../../../api/certificationPreparation';
import type { TrainPeople, TrainPeopleQuery } from '../../../types/training';
import { PermissionGate } from '../../../directives/permission';
import type {
  PreparationCandidate,
  PreparationCandidatePayload,
  PreparationCandidateQuery,
  PreparationCandidateStaging,
  PreparationCandidateStagingQuery,
  PreparationImportBatch,
  PreparationImportError,
} from '../../../types/certificationPreparation';
import { CandidateDocumentDrawer } from './CandidateDocumentDrawer';
import { candidateDocumentRatioTag, dash, dateTimeText, levelOptions, statusTag } from './shared';

type ImportChannel = 'staging' | 'training' | 'other';

const certificationLevelToTrainingLevel: Record<string, number> = {
  junior: 0,
  intermediate: 1,
  senior: 2,
  technician: 3,
  senior_technician: 4,
};

interface CandidateImportProps {
  taskId?: string;
  onChanged?: () => void;
}

export function CandidateImport({ taskId, onChanged }: CandidateImportProps) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState<PreparationCandidateQuery>({ page: 1, pageSize: 10 });
  const [importChannel, setImportChannel] = useState<ImportChannel>('other');
  const [selectedScopeId, setSelectedScopeId] = useState<string>();
  const [candidateModalOpen, setCandidateModalOpen] = useState(false);
  const [editingCandidate, setEditingCandidate] = useState<PreparationCandidate | null>(null);
  const [documentCandidate, setDocumentCandidate] = useState<PreparationCandidate | null>(null);
  const [lastImportBatch, setLastImportBatch] = useState<PreparationImportBatch | null>(null);
  type CandidateFormValues = Omit<PreparationCandidatePayload, 'graduateDate' | 'workStartDate'> & {
    graduateDate?: Dayjs;
    workStartDate?: Dayjs;
  };
  const [candidateForm] = Form.useForm<CandidateFormValues>();
  const [importFileList, setImportFileList] = useState<UploadFile[]>([]);
  const [importRawFile, setImportRawFile] = useState<File | null>(null);
  const [trainingQuery, setTrainingQuery] = useState<TrainPeopleQuery>({ page: 1, pageSize: 10, type: 'student' });
  const [selectedTrainingPeople, setSelectedTrainingPeople] = useState<Map<number, TrainPeople>>(new Map());
  const [stagingQuery, setStagingQuery] = useState<PreparationCandidateStagingQuery>({ page: 1, pageSize: 10 });
  const [selectedStagingRows, setSelectedStagingRows] = useState<Map<string, PreparationCandidateStaging>>(new Map());

  const candidatesQuery = useQuery({
    queryKey: ['certification-preparation-candidates', taskId, query],
    queryFn: () => pagePreparationCandidates(taskId!, query),
    enabled: Boolean(taskId),
  });

  const taskQuery = useQuery({
    queryKey: ['certification-task-import-scopes', taskId],
    queryFn: () => getCertificationTask(taskId!),
    enabled: Boolean(taskId),
  });
  const scopeOptions = (taskQuery.data?.scopes || []).map((scope) => ({
    value: scope.id,
    label: `${scope.workTypeName || '未设置工种'}｜${scope.levelName || '未设置等级'}`,
  }));
  const selectedScope = (taskQuery.data?.scopes || []).find((scope) => scope.id === selectedScopeId);
  const selectedTrainingLevel = selectedScope?.levelCode
    ? certificationLevelToTrainingLevel[selectedScope.levelCode]
    : undefined;

  const stagingPeopleQuery = useQuery({
    queryKey: ['certification-preparation-candidate-staging', taskId, stagingQuery],
    queryFn: () => pageCandidateStaging(taskId!, stagingQuery),
    enabled: Boolean(taskId) && importChannel === 'staging',
  });

  const trainingPeopleQuery = useQuery({
    queryKey: ['training-people-import', trainingQuery, selectedScopeId],
    queryFn: () => pagePeople({
      ...trainingQuery,
      applyWorkTypeName: selectedScope!.workTypeName,
      applyLevel: selectedTrainingLevel,
    }),
    enabled: Boolean(taskId)
      && importChannel === 'training'
      && Boolean(selectedScope?.workTypeName)
      && selectedTrainingLevel !== undefined,
  });

  const importedCandidatesQuery = useQuery({
    queryKey: ['certification-preparation-candidates-imported', taskId],
    queryFn: () => listAllPreparationCandidates(taskId!, { status: 'active' }),
    enabled: Boolean(taskId) && importChannel === 'training',
  });

  const importedTrainingKeys = useMemo(() => {
    const keys = new Set<string>();
    (importedCandidatesQuery.data?.items || []).forEach((candidate) => {
      if (candidate.idCardNo) {
        keys.add(`id:${candidate.idCardNo}`);
      }
      if (candidate.phone) {
        keys.add(`phone:${candidate.phone}`);
      }
    });
    return keys;
  }, [importedCandidatesQuery.data?.items]);

  const trainingPeople = (trainingPeopleQuery.data?.items || []).filter((person) => {
    if (person.idCard && importedTrainingKeys.has(`id:${person.idCard}`)) {
      return false;
    }
    if (person.phone && importedTrainingKeys.has(`phone:${person.phone}`)) {
      return false;
    }
    return true;
  });

  const errorQuery = useQuery({
    queryKey: ['certification-preparation-import-errors', taskId, lastImportBatch?.id],
    queryFn: () => pagePreparationImportErrors(taskId!, lastImportBatch!.id, { page: 1, pageSize: 50 }),
    enabled: Boolean(taskId && lastImportBatch?.id && (lastImportBatch.failedCount || 0) > 0),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates-imported', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidate-staging', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates-select', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-documents', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    onChanged?.();
  };

  const createMutation = useMutation({
    mutationFn: (payload: PreparationCandidatePayload) => createPreparationCandidate(taskId!, payload),
    onSuccess: () => {
      message.success('学员已新增');
      setCandidateModalOpen(false);
      candidateForm.resetFields();
      refresh();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PreparationCandidatePayload }) =>
      updatePreparationCandidate(taskId!, id, payload),
    onSuccess: () => {
      message.success('学员已更新');
      setCandidateModalOpen(false);
      setEditingCandidate(null);
      candidateForm.resetFields();
      refresh();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (candidateId: string) => deletePreparationCandidate(taskId!, candidateId),
    onSuccess: () => {
      message.success('学员已删除');
      refresh();
    },
  });

  const promoteMutation = useMutation({
    mutationFn: (stagingIds: string[]) => promoteCandidateStaging(taskId!, { stagingIds }),
    onSuccess: (result) => {
      message.success(`导入完成：成功 ${result.successCount}，失败 ${result.failedCount}`);
      setSelectedStagingRows(new Map());
      refresh();
    },
    onError: (error: Error) => {
      message.error(error.message || '导入失败');
    },
  });

  const importMutation = useMutation({
    mutationFn: (payload: {
      sourceType: 'training';
      trainingPeopleIds?: number[];
    }) => {
      if (!selectedScopeId) {
        throw new Error('请选择认定班级（工种和等级）');
      }
      if (!payload.trainingPeopleIds?.length) {
        throw new Error('请勾选要导入的培训学员');
      }
      return importPreparationCandidates(taskId!, {
        sourceType: payload.sourceType,
        trainingPeopleIds: payload.trainingPeopleIds,
        taskScopeId: selectedScopeId,
      });
    },
    onSuccess: (batch) => {
      message.success(`导入完成：成功 ${batch.successCount || 0}，失败 ${batch.failedCount || 0}`);
      setLastImportBatch(batch);
      setImportFileList([]);
      setSelectedTrainingPeople(new Map());
      refresh();
    },
    onError: (error: Error) => {
      message.error(error.message || '导入失败');
    },
  });

  const excelImportMutation = useMutation({
    mutationFn: (file: File) => {
      const scopeId = selectedScopeId || scopeOptions[0]?.value;
      if (!scopeId) throw new Error('当前任务尚未配置认定班级');
      return importPreparationCandidatesExcel(taskId!, scopeId, file);
    },
    onSuccess: (batch) => {
      message.success(`XLSX 导入完成：成功 ${batch.successCount || 0}，失败 ${batch.failedCount || 0}`);
      setLastImportBatch(batch);
      setImportFileList([]);
      setImportRawFile(null);
      refresh();
    },
    onError: (error: Error) => message.error(error.message || 'XLSX 导入失败'),
  });

  const generateBatchMaterialsMutation = useMutation({
    mutationFn: (batchId: string) => generatePreparationImportBatchMaterials(taskId!, batchId),
    onSuccess: (batch) => {
      message.success(`申报材料已生成：${batch.successCount || 0} 名考生`);
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-materials', taskId] });
    },
    onError: (error: Error) => message.error(error.message || '申报材料生成失败，请先补齐并审核必传资料'),
  });

  const stagingPeopleColumns: ColumnsType<PreparationCandidateStaging> = [
    { title: '姓名', dataIndex: 'candidateName', width: 100, render: dash },
    { title: '性别', dataIndex: 'gender', width: 72, render: dash },
    { title: '身份证号', dataIndex: 'idCardNo', width: 170, ellipsis: true, render: dash },
    { title: '手机号', dataIndex: 'phone', width: 130, render: dash },
    { title: '报考等级', dataIndex: 'applyLevelName', width: 130, render: dash },
    { title: '工作单位', dataIndex: 'companyName', minWidth: 180, ellipsis: true, render: dash },
  ];

  const trainingPeopleColumns: ColumnsType<TrainPeople> = [
    { title: '姓名', dataIndex: 'realname', width: 100, render: dash },
    { title: '手机号', dataIndex: 'phone', width: 130, render: dash },
    { title: '身份证', dataIndex: 'idCard', width: 170, ellipsis: true, render: dash },
    { title: '培训ID', dataIndex: 'trainId', width: 90, render: dash },
    { title: '班级ID', dataIndex: 'trainClassId', width: 90, render: dash },
  ];

  function handleStagingSelectionChange(_keys: Key[], rows: PreparationCandidateStaging[]) {
    setSelectedStagingRows((prev) => {
      const next = new Map(prev);
      const currentPageIds = (stagingPeopleQuery.data?.items || []).map((row) => row.id);
      currentPageIds.forEach((id) => next.delete(id));
      rows.forEach((row) => next.set(row.id, row));
      return next;
    });
  }

  function submitStagingPromote() {
    const stagingIds = Array.from(selectedStagingRows.keys());
    if (stagingIds.length === 0) {
      message.warning('请勾选要导入的测试学员');
      return;
    }
    promoteMutation.mutate(stagingIds);
  }

  function handleTrainingSelectionChange(_keys: Key[], rows: TrainPeople[]) {
    setSelectedTrainingPeople((prev) => {
      const next = new Map(prev);
      const currentPageIds = trainingPeople.map((person) => person.id);
      currentPageIds.forEach((id) => next.delete(id));
      rows.forEach((row) => next.set(row.id, row));
      return next;
    });
  }

  function submitTrainingImport() {
    const selectedPeople = Array.from(selectedTrainingPeople.values());
    if (selectedPeople.some((person) => !person.realname || !person.idCard)) {
      message.warning('所选培训学员缺少姓名或身份证号，无法导入');
      return;
    }
    importMutation.mutate({
      sourceType: 'training',
      trainingPeopleIds: selectedPeople.map((person) => person.id),
    });
  }

  function submitOtherImport() {
    const file = importRawFile;
    if (!file) {
      message.warning('请先选择要导入的 XLSX 文件');
      return;
    }
    excelImportMutation.mutate(file);
  }

  async function downloadExcelTemplate() {
    const response = await downloadPreparationCandidateExcelTemplate();
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = '考生信息批量导入模板.xlsx';
    link.click();
    URL.revokeObjectURL(url);
  }

  const columns: ColumnsType<PreparationCandidate> = useMemo(
    () => [
      { title: '姓名', dataIndex: 'candidateName', width: 110, fixed: 'left' },
      { title: '准考证号', dataIndex: 'admissionTicketNo', width: 130, render: dash },
      { title: '身份证号', dataIndex: 'maskedIdCardNo', width: 180, render: (value, record) => value || record.idCardNo || '-' },
      { title: '手机号', dataIndex: 'maskedPhone', width: 140, render: (value, record) => value || record.phone || '-' },
      { title: '等级', dataIndex: 'levelName', width: 100, render: dash },
      { title: '资料进度', width: 110, align: 'center', render: (_, record) => candidateDocumentRatioTag(record) },
      { title: '资料齐全', dataIndex: 'documentComplete', width: 90, render: (value: boolean) => (value ? '是' : '否') },
      { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
      { title: '创建时间', dataIndex: 'createdAt', width: 170, render: dateTimeText },
      {
        title: '操作',
        width: 120,
        align: 'center',
        fixed: 'right',
        render: (_, record) => (
          <Space size={2}>
            <Button
              type="link"
              size="small"
              title="上传资料"
              aria-label="上传资料"
              icon={<FileProtectOutlined />}
              onClick={() => setDocumentCandidate(record)}
              style={{ paddingInline: 5 }}
            >
            </Button>
            <PermissionGate code="certification.preparation.candidate.edit">
              <Button
                type="link"
                size="small"
                title="编辑"
                aria-label="编辑"
                icon={<EditOutlined />}
                onClick={() => openEdit(record)}
                style={{ paddingInline: 5 }}
              >
              </Button>
            </PermissionGate>
            <PermissionGate code="certification.preparation.candidate.delete">
              <Popconfirm
                title="确认删除该学员？"
                okText="删除"
                cancelText="取消"
                onConfirm={() => deleteMutation.mutate(record.id)}
              >
                <Button
                  type="link"
                  size="small"
                  danger
                  title="删除"
                  aria-label="删除"
                  icon={<DeleteOutlined />}
                  style={{ paddingInline: 5 }}
                >
                </Button>
              </Popconfirm>
            </PermissionGate>
          </Space>
        ),
      },
    ],
    [deleteMutation],
  );

  const errorColumns: ColumnsType<PreparationImportError> = [
    { title: '行号', dataIndex: 'rowNo', width: 80 },
    { title: '姓名', dataIndex: 'candidateName', width: 110, render: dash },
    { title: '身份证号', dataIndex: 'idCardNo', width: 180, render: dash },
    { title: '手机号', dataIndex: 'phone', width: 140, render: dash },
    { title: '失败原因', dataIndex: 'errorMessage', render: dash },
  ];

  function openCreate() {
    setEditingCandidate(null);
    candidateForm.resetFields();
    candidateForm.setFieldsValue({ importSource: 'other' });
    setCandidateModalOpen(true);
  }

  async function openEdit(record: PreparationCandidate) {
    setEditingCandidate(record);
    candidateForm.resetFields();
    candidateForm.setFieldsValue({
      candidateName: record.candidateName,
      admissionTicketNo: record.admissionTicketNo,
      idCardNo: record.idCardNo || record.maskedIdCardNo || '',
      phone: record.phone || record.maskedPhone,
      levelCode: record.levelCode,
      levelName: record.levelName,
      importSource: record.importSource || 'other',
      orgName: record.orgName,
      taskScopeId: record.taskScopeId,
      remark: undefined,
    });
    setCandidateModalOpen(true);
    try {
      const source = await getPreparationCandidateApplicationSource(taskId!, record.id);
      if (!source) {
        return;
      }
      candidateForm.setFieldsValue({
        gender: source.gender,
        education: source.education,
        graduateDate: source.graduateDate ? dayjs(source.graduateDate) : undefined,
        companyAddress: source.companyAddress,
        zipCode: source.zipCode,
        workStartDate: source.workStartDate ? dayjs(source.workStartDate) : undefined,
        originalWorkTypeName: source.originalWorkTypeName,
        originalLevelName: source.originalLevelName,
        originalCertNo: source.originalCertNo,
        applyWorkYears: source.applyWorkYears,
        companyCreditCode: source.companyCreditCode,
        workTypeCode: source.workTypeCode,
        workDirection: source.workDirection,
        workResume: source.workResume,
        trainingSummary: source.trainingSummary,
        homeAddress: source.homeAddress,
        school: source.school,
        major: source.major,
        nation: source.nation,
        orgName: source.companyName || record.orgName,
        remark: source.remark,
      });
    } catch {
      // Editing still works with candidate base fields when source snapshot is absent.
    }
  }

  function toDateString(value: unknown): string | undefined {
    if (!value) {
      return undefined;
    }
    if (dayjs.isDayjs(value)) {
      return value.format('YYYY-MM-DD');
    }
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
    return undefined;
  }

  function submitCandidate() {
    candidateForm.validateFields().then((values) => {
      const payload: PreparationCandidatePayload = {
        ...values,
        importSource: values.importSource || 'other',
        graduateDate: toDateString(values.graduateDate),
        workStartDate: toDateString(values.workStartDate),
      };
      if (editingCandidate) {
        updateMutation.mutate({ id: editingCandidate.id, payload });
        return;
      }
      createMutation.mutate(payload);
    });
  }

  if (!taskId) {
    return (
      <ProCard>
        <Typography.Text type="secondary">请选择认定任务后维护学员。</Typography.Text>
      </ProCard>
    );
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard>
        <Segmented<ImportChannel>
          value={importChannel}
          onChange={(value) => {
            setImportChannel(value);
            setImportFileList([]);
            setImportRawFile(null);
            setSelectedTrainingPeople(new Map());
            setSelectedStagingRows(new Map());
          }}
          options={[
            { label: '认定途径', value: 'other' },
            { label: '培训途径', value: 'training' },
          ]}
          style={{ marginBottom: 16 }}
        />

        {importChannel === 'staging' ? (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              从测试学员池勾选后导入当前认定任务；导入后仅需上传证件照，系统可自动生成报名信息表。
            </Typography.Text>
            <Input.Search
              allowClear
              placeholder="搜索姓名、身份证、手机号"
              onSearch={(keyword) => setStagingQuery((prev) => ({ ...prev, keyword, page: 1 }))}
            />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              已选 {selectedStagingRows.size} 人，勾选后点击「确认导入」。
            </Typography.Text>
            <Table
              rowKey="id"
              size="small"
              columns={stagingPeopleColumns}
              dataSource={stagingPeopleQuery.data?.items || []}
              loading={stagingPeopleQuery.isFetching}
              scroll={{ y: 240 }}
              rowSelection={{
                selectedRowKeys: Array.from(selectedStagingRows.keys()),
                onChange: handleStagingSelectionChange,
                preserveSelectedRowKeys: true,
              }}
              pagination={{
                current: stagingQuery.page,
                pageSize: stagingQuery.pageSize,
                total: stagingPeopleQuery.data?.total || 0,
                showSizeChanger: true,
                onChange: (page, pageSize) => setStagingQuery((prev) => ({ ...prev, page, pageSize })),
              }}
            />
            <Space wrap>
              <PermissionGate code="certification.preparation.candidate.import">
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={promoteMutation.isPending}
                  onClick={submitStagingPromote}
                >
                  确认导入测试学员
                </Button>
              </PermissionGate>
            </Space>
          </Space>
        ) : importChannel === 'training' ? (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Select
              allowClear
              placeholder="选择认定班级（工种｜等级）"
              value={selectedScopeId}
              onChange={(value) => {
                setSelectedScopeId(value);
                setSelectedTrainingPeople(new Map());
                setTrainingQuery((prev) => ({ ...prev, page: 1 }));
              }}
              options={scopeOptions}
              loading={taskQuery.isFetching}
              style={{ minWidth: 280 }}
            />
            <Input.Search
              allowClear
              placeholder="搜索姓名、手机号"
              onSearch={(keyword) => setTrainingQuery((prev) => ({ ...prev, keyword, page: 1 }))}
            />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              已选 {selectedTrainingPeople.size} 人，勾选后点击「确认导入」。
            </Typography.Text>
            <Table
              rowKey="id"
              size="small"
              columns={trainingPeopleColumns}
              dataSource={trainingPeople}
              loading={trainingPeopleQuery.isFetching}
              scroll={{ y: 240 }}
              rowSelection={{
                selectedRowKeys: Array.from(selectedTrainingPeople.keys()),
                onChange: handleTrainingSelectionChange,
                preserveSelectedRowKeys: true,
              }}
              pagination={{
                current: trainingQuery.page,
                pageSize: trainingQuery.pageSize,
                total: trainingPeopleQuery.data?.total || 0,
                showSizeChanger: true,
                onChange: (page, pageSize) => setTrainingQuery((prev) => ({ ...prev, page, pageSize })),
              }}
            />
            <Space wrap>
              <PermissionGate code="certification.preparation.candidate.import">
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={importMutation.isPending}
                  onClick={submitTrainingImport}
                >
                  确认导入培训学员
                </Button>
              </PermissionGate>
            </Space>
          </Space>
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Space wrap>
              <Button size="small" icon={<DownloadOutlined />} onClick={downloadExcelTemplate}>
                下载模板
              </Button>
              <Upload
                accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                maxCount={1}
                fileList={importFileList}
                beforeUpload={(file) => {
                  setImportFileList([file as UploadFile]);
                  setImportRawFile(file);
                  return false;
                }}
                onRemove={() => {
                  setImportFileList([]);
                  setImportRawFile(null);
                }}
              >
                <Button size="small" icon={<UploadOutlined />}>选择文件</Button>
              </Upload>
              <PermissionGate code="certification.preparation.candidate.import">
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={excelImportMutation.isPending}
                  onClick={submitOtherImport}
                >
                  确认导入其他学员
                </Button>
              </PermissionGate>
              <PermissionGate code="certification.preparation.candidate.edit">
                <Button icon={<PlusOutlined />} onClick={openCreate}>
                  新增学员
                </Button>
              </PermissionGate>
            </Space>
          </Space>
        )}
      </ProCard>

      <ProCard
        title="学员列表"
        extra={<Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>}
      >
        <Form
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={(values) => setQuery({ ...query, ...values, page: 1 })}
        >
          <Form.Item name="keyword">
            <Input.Search allowClear placeholder="姓名、身份证、手机号" style={{ width: 260 }} />
          </Form.Item>
          <Form.Item name="taskScopeId">
            <Select
              allowClear
              placeholder="选择认定班级（工种｜级别）"
              style={{ width: 280 }}
              options={scopeOptions}
            />
          </Form.Item>
          <Form.Item name="levelCode">
            <Select allowClear placeholder="等级" style={{ width: 140 }} options={levelOptions} />
          </Form.Item>
          <Button htmlType="submit">查询</Button>
        </Form>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={candidatesQuery.data?.items || []}
          loading={candidatesQuery.isFetching}
          scroll={{ x: 1420 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: candidatesQuery.data?.total || 0,
            showSizeChanger: true,
            onChange: (page, pageSize) => setQuery({ ...query, page, pageSize }),
          }}
        />
      </ProCard>

      {lastImportBatch && (
        <ProCard
          title={`最近导入批次：${lastImportBatch.batchNo}`}
          collapsible
          defaultCollapsed={(lastImportBatch.failedCount || 0) === 0}
        >
          <Space style={{ marginBottom: 12 }}>
            <Typography.Text>总数 {lastImportBatch.totalCount || 0}</Typography.Text>
            <Typography.Text type="success">成功 {lastImportBatch.successCount || 0}</Typography.Text>
            <Typography.Text type="danger">失败 {lastImportBatch.failedCount || 0}</Typography.Text>
            {statusTag(lastImportBatch.status)}
            {lastImportBatch.sourceType === 'other_xlsx' && (lastImportBatch.successCount || 0) > 0 ? (
              <PermissionGate code="certification.preparation.material.generate">
                <Button
                  type="primary"
                  size="small"
                  loading={generateBatchMaterialsMutation.isPending}
                  onClick={() => generateBatchMaterialsMutation.mutate(lastImportBatch.id)}
                >
                  生成本批申报材料
                </Button>
              </PermissionGate>
            ) : null}
          </Space>
          <Table
            rowKey="id"
            size="small"
            columns={errorColumns}
            dataSource={errorQuery.data?.items || []}
            pagination={false}
          />
        </ProCard>
      )}

      <CandidateDocumentDrawer
        taskId={taskId}
        candidate={documentCandidate}
        open={Boolean(documentCandidate)}
        onClose={() => setDocumentCandidate(null)}
        onChanged={onChanged}
      />

      <Modal
        title={editingCandidate ? '编辑学员' : '新增学员（其他学员）'}
        open={candidateModalOpen}
        onOk={submitCandidate}
        onCancel={() => setCandidateModalOpen(false)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        width={960}
        styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}
      >
        <Form form={candidateForm} layout="vertical">
          <Typography.Text type="secondary">
            以下字段会写入职业技能等级认定申请表；未填写的项生成申请表时保持空白。
          </Typography.Text>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12, marginTop: 12 }}>
            <Form.Item name="candidateName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="admissionTicketNo" label="准考证号">
              <Input maxLength={64} />
            </Form.Item>
            <Form.Item
              name="idCardNo"
              label="身份证号"
              rules={[{ required: true, pattern: /^\d{17}[0-9Xx]$/, message: '请输入18位身份证号' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item name="gender" label="性别">
              <Select
                allowClear
                options={[
                  { value: '男', label: '男' },
                  { value: '女', label: '女' },
                ]}
                placeholder="可留空，默认按身份证识别"
              />
            </Form.Item>
            <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="nation" label="民族">
              <Input placeholder="如：汉族" />
            </Form.Item>
            <Form.Item name="education" label="文化程度">
              <Input placeholder="如：初中/高中/大专/本科" />
            </Form.Item>
            <Form.Item name="graduateDate" label="毕业时间">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="school" label="毕业学校">
              <Input />
            </Form.Item>
            <Form.Item name="major" label="所学专业">
              <Input placeholder="初中、高中学历可填无" />
            </Form.Item>
            <Form.Item name="homeAddress" label="身份证地址">
              <Input />
            </Form.Item>
            <Form.Item name="taskScopeId" label="认定班级（工种/等级）">
              <Select allowClear options={scopeOptions} placeholder="用于填入申报/认定职业与等级" />
            </Form.Item>
            <Form.Item name="levelCode" label="等级">
              <Select
                allowClear
                options={levelOptions}
                onChange={(_, option) => {
                  const item = Array.isArray(option) ? option[0] : option;
                  candidateForm.setFieldValue('levelName', item?.label);
                }}
              />
            </Form.Item>
            <Form.Item name="levelName" label="等级名称">
              <Input />
            </Form.Item>
            <Form.Item name="orgName" label="工作单位">
              <Input />
            </Form.Item>
            <Form.Item name="companyAddress" label="单位地址">
              <Input />
            </Form.Item>
            <Form.Item name="zipCode" label="邮编">
              <Input maxLength={12} />
            </Form.Item>
            <Form.Item name="companyCreditCode" label="统一社会信用代码">
              <Input />
            </Form.Item>
            <Form.Item name="workStartDate" label="参加工作时间">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="applyWorkYears" label="申报工种工龄">
              <Input placeholder="如：5年" />
            </Form.Item>
            <Form.Item name="workTypeCode" label="职业代码">
              <Input />
            </Form.Item>
            <Form.Item name="workDirection" label="工种方向">
              <Input />
            </Form.Item>
            <Form.Item name="originalWorkTypeName" label="原职业（工种）">
              <Input />
            </Form.Item>
            <Form.Item name="originalLevelName" label="原技术等级">
              <Input />
            </Form.Item>
            <Form.Item name="originalCertNo" label="原证书编号">
              <Input />
            </Form.Item>
            <Form.Item name="importSource" label="导入途径" hidden>
              <Input />
            </Form.Item>
            <Form.Item name="workResume" label="个人工作简历" style={{ gridColumn: '1 / -1' }}>
              <Input.TextArea rows={3} placeholder="有则如实填写；空则申请表对应栏留空" />
            </Form.Item>
            <Form.Item name="trainingSummary" label="参加培训情况" style={{ gridColumn: '1 / -1' }}>
              <Input.TextArea rows={3} placeholder="时间、期限、内容等；空则申请表对应栏留空" />
            </Form.Item>
            <Form.Item name="remark" label="备注" style={{ gridColumn: '1 / -1' }}>
              <Input />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </Space>
  );
}
