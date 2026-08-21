import { useMemo, useState, type Key } from 'react';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  DownloadOutlined,
  EyeOutlined,
  FileZipOutlined,
  NotificationOutlined,
  ReloadOutlined,
  SearchOutlined,
  TrophyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';

type Props = {
  processCurrent?: number;
  pageTitle?: string;
  pageSubtitle?: string;
};

function classStatusTag(status?: string) {
  if (status === 'done') return <Tag color="success">已结业</Tag>;
  if (status === 'partial') return <Tag color="warning">部分结业</Tag>;
  return <Tag>未结业</Tag>;
}

function studentStatusTag(status?: number) {
  if (status === 1) return <Tag color="success">已结业</Tag>;
  if (status === 2) return <Tag color="error">不合格结业</Tag>;
  return <Tag>未结业</Tag>;
}

async function downloadLedger(classId?: number) {
  try {
    await trainApi.exportGraduationLedger(classId);
  } catch (e) {
    message.error((e as Error).message || '导出失败');
  }
}

export function TrainingGraduationPage({
  processCurrent = 7,
  pageTitle = '毕业典礼',
  pageSubtitle = '班级结业打标、站内信通知、台账导出与材料归档；证书模板轻关联可查',
}: Props) {
  const queryClient = useQueryClient();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<{
    page: number;
    pageSize: number;
    keyword?: string;
    graduateStatus?: string;
  }>({ page: 1, pageSize: 20 });
  const [studentModalOpen, setStudentModalOpen] = useState(false);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [templateId, setTemplateId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['graduation-candidates', query],
    queryFn: () => trainApi.listGraduationCandidates(query),
  });

  const { data: studentData, isLoading: studentLoading } = useQuery({
    queryKey: ['graduation-students', selectedClassId],
    queryFn: () => trainApi.listGraduationStudents(selectedClassId!),
    enabled: !!selectedClassId,
  });

  const { data: notices } = useQuery({
    queryKey: ['graduation-notices', selectedClassId],
    queryFn: () => trainApi.listGraduationNotices(selectedClassId!),
    enabled: !!selectedClassId && studentModalOpen,
  });

  const { data: archives } = useQuery({
    queryKey: ['graduation-archives', selectedClassId],
    queryFn: () => trainApi.listGraduationArchives(selectedClassId!),
    enabled: !!selectedClassId && studentModalOpen,
  });

  const { data: fileTemps } = useQuery({
    queryKey: ['file-temps-graduation'],
    queryFn: () => trainApi.listFileTemps({ type: 1 }),
  });

  const invalidateClass = () => {
    void queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] });
    void queryClient.invalidateQueries({ queryKey: ['graduation-students', selectedClassId] });
    void queryClient.invalidateQueries({ queryKey: ['graduation-notices', selectedClassId] });
    void queryClient.invalidateQueries({ queryKey: ['graduation-archives', selectedClassId] });
  };

  const graduateMu = useMutation({
    mutationFn: (payload: Parameters<typeof trainApi.graduateClass>[1]) =>
      trainApi.graduateClass(selectedClassId!, payload),
    onSuccess: (res) => {
      message.success(`结业完成：变更 ${res.changedCount ?? res.graduateCount ?? 0} 人`);
      setSelectedRowKeys([]);
      invalidateClass();
    },
    onError: (e: Error) => message.error(e.message || '结业失败'),
  });

  const notifyMu = useMutation({
    mutationFn: () => trainApi.notifyGraduation(selectedClassId!),
    onSuccess: (res) => {
      message.success(res.message || '通知已发送');
      invalidateClass();
    },
    onError: (e: Error) => message.error(e.message || '发送失败'),
  });

  const bindTplMu = useMutation({
    mutationFn: () => trainApi.bindGraduationCertificateTemplate(selectedClassId!, templateId),
    onSuccess: () => {
      message.success('证书模板已绑定');
      invalidateClass();
    },
    onError: (e: Error) => message.error(e.message || '绑定失败'),
  });

  const issueMu = useMutation({
    mutationFn: () => trainApi.batchIssueGraduationCertificates(selectedClassId!),
    onSuccess: (res) => {
      message.success(`发证记录：新建 ${res.created}，跳过 ${res.skipped}`);
      invalidateClass();
    },
    onError: (e: Error) => message.error(e.message || '发证失败'),
  });

  const archiveMu = useMutation({
    mutationFn: () => trainApi.createGraduationArchive(selectedClassId!),
    onSuccess: (res) => {
      message.success(`归档包已生成 v${res.versionNo}`);
      invalidateClass();
      if (res.filePath) window.open(res.filePath, '_blank');
    },
    onError: (e: Error) => message.error(e.message || '归档失败'),
  });

  const openStudents = (classId: number, certTempId?: number | null) => {
    setSelectedClassId(classId);
    setTemplateId(certTempId ?? null);
    setSelectedRowKeys([]);
    setStudentModalOpen(true);
  };

  const handleSearch = () => {
    const values = searchForm.getFieldsValue();
    setQuery({
      page: 1,
      pageSize: query.pageSize,
      keyword: values.keyword || undefined,
      graduateStatus: values.graduateStatus || undefined,
    });
  };

  const columns: ColumnsType<Record<string, unknown>> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '班级名称', dataIndex: 'title', width: 180, ellipsis: true },
    { title: '培训项目', dataIndex: 'trainTitle', width: 180, ellipsis: true },
    { title: '企业', dataIndex: 'companyName', width: 140, ellipsis: true },
    { title: '工种', dataIndex: 'workTypeName', width: 120, ellipsis: true },
    {
      title: '结业进度',
      width: 110,
      render: (_, r) => `${r.graduatedCount ?? 0}/${r.studentCount ?? 0}`,
    },
    {
      title: '状态',
      dataIndex: 'graduateStatus',
      width: 110,
      render: (v?: string) => classStatusTag(v),
    },
    { title: '开始', dataIndex: 'dateStart', width: 110 },
    { title: '结束', dataIndex: 'dateEnd', width: 110 },
    {
      title: '操作',
      key: 'act',
      width: 280,
      fixed: 'right',
      render: (_, r) => (
        <Space size="small" wrap>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => openStudents(r.id as number, r.certificateTempId as number | null)}
          >
            学员与结业
          </Button>
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => void downloadLedger(r.id as number)}
          >
            导出台账
          </Button>
        </Space>
      ),
    },
  ];

  const studentColumns: ColumnsType<Record<string, unknown>> = useMemo(
    () => [
      { title: '姓名', dataIndex: 'realname', width: 90 },
      { title: '手机', dataIndex: 'phone', width: 120 },
      {
        title: '理论',
        dataIndex: 'theoryScore',
        width: 70,
        render: (v) => (v == null || v === '' ? '—' : v),
      },
      {
        title: '技能',
        dataIndex: 'skillScore',
        width: 70,
        render: (v, r) => {
          const score = v ?? r.realScore;
          return score == null || score === '' ? '—' : score;
        },
      },
      {
        title: '综合',
        dataIndex: 'examScore',
        width: 70,
        render: (v) => (v == null || v === '' ? '—' : v),
      },
      {
        title: '工作业绩',
        dataIndex: 'performanceScore',
        width: 90,
        render: (v) => (v == null || v === '' ? '—' : v),
      },
      {
        title: '达标',
        dataIndex: 'qualified',
        width: 90,
        render: (v: boolean, r) =>
          v ? <Tag color="success">达标</Tag> : <Tag color="warning">{String(r.qualifyHint || '未达标')}</Tag>,
      },
      {
        title: '结业状态',
        dataIndex: 'graduateStatus',
        width: 110,
        render: (v?: number) => studentStatusTag(v),
      },
      {
        title: '证书',
        width: 140,
        render: (_, r) =>
          r.hasCertificate ? (
            <Typography.Text>{String(r.certificateNo || '已建证')}</Typography.Text>
          ) : (
            <Typography.Text type="secondary">未建证</Typography.Text>
          ),
      },
      {
        title: '模板',
        dataIndex: 'templateTitle',
        width: 120,
        ellipsis: true,
        render: (v?: string) => v || '—',
      },
    ],
    [],
  );

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>{pageTitle}</Typography.Title>
          <Typography.Text type="secondary">{pageSubtitle}</Typography.Text>
        </div>
      </section>

      <TrainingProcessSteps current={processCurrent} />

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="结业以班级为粒度打标；项目完成状态仅在该培训下全部班级结业后回写。通知走站内信并落库。"
      />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="keyword">
            <Input
              placeholder="搜索班级名称..."
              prefix={<SearchOutlined />}
              allowClear
              onPressEnter={handleSearch}
              style={{ width: 240 }}
            />
          </Form.Item>
          <Form.Item name="graduateStatus">
            <Select
              allowClear
              placeholder="结业状态"
              style={{ width: 140 }}
              options={[
                { value: 'pending', label: '未结业' },
                { value: 'partial', label: '部分结业' },
                { value: 'done', label: '已结业' },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSearch}>
              查询
            </Button>
          </Form.Item>
          <Form.Item>
            <Button icon={<DownloadOutlined />} onClick={() => void downloadLedger()}>
              导出全部台账
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] })}
          >
            刷新
          </Button>
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={isLoading}
          columns={columns}
          dataSource={(data?.list ?? []) as Record<string, unknown>[]}
          scroll={{ x: 1300 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: data?.total ?? 0,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }),
          }}
        />
      </Card>

      <Modal
        title={studentData?.classTitle ? `结业办理 · ${studentData.classTitle}` : '结业办理'}
        open={studentModalOpen}
        onCancel={() => {
          setStudentModalOpen(false);
          setSelectedClassId(null);
        }}
        footer={null}
        width={1080}
        destroyOnHidden
      >
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            style={{ width: 260 }}
            placeholder="绑定结业证书模板"
            allowClear
            value={templateId ?? undefined}
            options={(fileTemps?.list ?? []).map((t) => ({ value: t.id, label: t.title }))}
            onChange={(v) => setTemplateId(v ?? null)}
          />
          <Button loading={bindTplMu.isPending} onClick={() => bindTplMu.mutate()}>
            保存模板绑定
          </Button>
          <Popconfirm
            title="对勾选学员执行结业？"
            description="未勾选则结业全班未处理学员；不合格也可标记（不发证）"
            onConfirm={() =>
              graduateMu.mutate(
                selectedRowKeys.length
                  ? { mode: 'selected', peopleIds: selectedRowKeys.map(Number), allowUnqualified: true }
                  : { mode: 'all', allowUnqualified: true },
              )
            }
          >
            <Button type="primary" icon={<TrophyOutlined />} loading={graduateMu.isPending}>
              执行结业
            </Button>
          </Popconfirm>
          <Button
            icon={<NotificationOutlined />}
            loading={notifyMu.isPending}
            onClick={() => notifyMu.mutate()}
          >
            发送站内信
          </Button>
          <Button loading={issueMu.isPending} onClick={() => issueMu.mutate()}>
            批量建证记录
          </Button>
          <Button
            icon={<FileZipOutlined />}
            loading={archiveMu.isPending}
            onClick={() => archiveMu.mutate()}
          >
            生成归档包
          </Button>
        </Space>

        <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
          班级状态 {classStatusTag(studentData?.graduateStatus)} · 模板{' '}
          {studentData?.templateTitle || '未绑定'} · 成绩与考试模块一致（理论/技能/综合/工作业绩）；达标=理论与技能均≥60；暂无成绩本期不强制
        </Typography.Paragraph>

        <Table
          rowKey="id"
          size="small"
          loading={studentLoading}
          columns={studentColumns}
          dataSource={(studentData?.list ?? []) as Record<string, unknown>[]}
          pagination={false}
          scroll={{ x: 1100, y: 360 }}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
            getCheckboxProps: (r) => ({
              disabled: r.graduateStatus === 1 || r.graduateStatus === 2,
            }),
          }}
        />

        {(notices?.length || archives?.length) ? (
          <Space direction="vertical" style={{ width: '100%', marginTop: 16 }} size="small">
            {!!notices?.length && (
              <Card size="small" title="通知历史">
                <Table
                  rowKey="id"
                  size="small"
                  pagination={false}
                  dataSource={notices}
                  columns={[
                    { title: '时间', dataIndex: 'createdAt', width: 170 },
                    { title: '标题', dataIndex: 'title', ellipsis: true },
                    {
                      title: '结果',
                      width: 140,
                      render: (_, r) => `成功 ${r.successCount ?? 0} / 失败 ${r.failCount ?? 0}`,
                    },
                  ]}
                />
              </Card>
            )}
            {!!archives?.length && (
              <Card size="small" title="归档版本">
                <Table
                  rowKey="id"
                  size="small"
                  pagination={false}
                  dataSource={archives}
                  columns={[
                    { title: '版本', dataIndex: 'versionNo', width: 80 },
                    { title: '时间', dataIndex: 'createdAt', width: 170 },
                    {
                      title: '下载',
                      render: (_, r) =>
                        r.filePath ? (
                          <a href={String(r.filePath)} target="_blank" rel="noreferrer">
                            下载
                          </a>
                        ) : (
                          '—'
                        ),
                    },
                  ]}
                />
              </Card>
            )}
          </Space>
        ) : null}
      </Modal>
    </div>
  );
}
