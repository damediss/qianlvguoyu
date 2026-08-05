import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Col,
  DatePicker,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Switch,
  Table,
  Tabs,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DownloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';

type ApiId = number | string;

const { RangePicker } = DatePicker;
const { Text } = Typography;

function fmtRate(v: number | null | undefined) {
  if (v == null) return '-';
  return `${v}%`;
}

function fmtTime(v?: string | null) {
  if (!v) return '-';
  return String(v).replace('T', ' ').slice(0, 19);
}

type PanelProps = {
  classId: ApiId | null;
  /** 为 false 时不发请求（如 Tab 未激活） */
  enabled?: boolean;
};

/** 班级刷题学情面板（跟班 Tab / Modal 共用） */
export function ClassPracticeAnalyticsPanel({ classId, enabled = true }: PanelProps) {
  const [range, setRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(29, 'day'),
    dayjs(),
  ]);
  const [inScopeOnly, setInScopeOnly] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [exporting, setExporting] = useState(false);

  const active = enabled && classId != null;

  const queryParams = useMemo(() => ({
    from: range[0].format('YYYY-MM-DD'),
    to: range[1].format('YYYY-MM-DD'),
    inScopeOnly,
  }), [range, inScopeOnly]);

  const handleExport = async (allTime: boolean) => {
    if (classId == null) return;
    setExporting(true);
    try {
      await trainApi.exportClassPracticeAnalytics(classId, {
        ...queryParams,
        allTime,
      });
      message.success(allTime ? '已导出全部刷题数据' : '已导出当前区间刷题数据');
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '导出失败');
    } finally {
      setExporting(false);
    }
  };

  const summaryQuery = useQuery({
    queryKey: ['class-practice-analytics-summary', classId, queryParams],
    queryFn: () => trainApi.getClassPracticeAnalyticsSummary(classId!, queryParams),
    enabled: active,
  });

  const studentsQuery = useQuery({
    queryKey: ['class-practice-analytics-students', classId, queryParams, keyword, page, pageSize],
    queryFn: () => trainApi.getClassPracticeAnalyticsStudents(classId!, {
      ...queryParams,
      keyword: keyword.trim() || undefined,
      page,
      pageSize,
    }),
    enabled: active,
  });

  const knowledgesQuery = useQuery({
    queryKey: ['class-practice-analytics-knowledges', classId, queryParams],
    queryFn: () => trainApi.getClassPracticeAnalyticsKnowledges(classId!, {
      ...queryParams,
      limit: 50,
    }),
    enabled: active,
  });

  const dailyQuery = useQuery({
    queryKey: ['class-practice-analytics-daily', classId, queryParams],
    queryFn: () => trainApi.getClassPracticeAnalyticsDaily(classId!, queryParams),
    enabled: active,
  });

  const summary = summaryQuery.data;

  const studentColumns: ColumnsType<trainApi.PracticeAnalyticsStudentRow> = [
    { title: '姓名', dataIndex: 'realname', width: 100, ellipsis: true },
    { title: '手机', dataIndex: 'phone', width: 120 },
    {
      title: 'App 账号',
      dataIndex: 'hasAccount',
      width: 90,
      render: (v: boolean) => (v ? '已开通' : '未开通'),
    },
    { title: '答题数', dataIndex: 'answerCount', width: 80, align: 'right' },
    { title: '正确数', dataIndex: 'rightCount', width: 80, align: 'right' },
    {
      title: '正确率',
      dataIndex: 'accuracyRate',
      width: 90,
      align: 'right',
      render: (v) => fmtRate(v),
    },
    { title: '练习次数', dataIndex: 'paperCount', width: 90, align: 'right' },
    { title: '练习天数', dataIndex: 'practiceDays', width: 90, align: 'right' },
    {
      title: '最近练习',
      dataIndex: 'lastPracticeAt',
      width: 160,
      render: (v) => fmtTime(v),
    },
    {
      title: '未练天数',
      dataIndex: 'idleDays',
      width: 90,
      align: 'right',
      render: (v) => (v == null ? '-' : v),
    },
  ];

  const knowledgeColumns: ColumnsType<trainApi.PracticeAnalyticsKnowledgeRow> = [
    { title: '知识点', dataIndex: 'title', ellipsis: true },
    { title: '答题数', dataIndex: 'answerCount', width: 90, align: 'right' },
    { title: '错误数', dataIndex: 'wrongCount', width: 90, align: 'right' },
    {
      title: '错误率',
      dataIndex: 'wrongRate',
      width: 90,
      align: 'right',
      render: (v) => fmtRate(v),
    },
    { title: '覆盖学员', dataIndex: 'userCount', width: 100, align: 'right' },
  ];

  const dailyColumns: ColumnsType<trainApi.PracticeAnalyticsDailyRow> = [
    { title: '日期', dataIndex: 'bizDate', width: 120 },
    { title: '答题数', dataIndex: 'answerCount', width: 90, align: 'right' },
    { title: '正确数', dataIndex: 'rightCount', width: 90, align: 'right' },
    {
      title: '正确率',
      dataIndex: 'accuracyRate',
      width: 90,
      align: 'right',
      render: (v) => fmtRate(v),
    },
    { title: '练习人数', dataIndex: 'userCount', width: 100, align: 'right' },
    { title: '练习次数', dataIndex: 'paperCount', width: 100, align: 'right' },
  ];

  return (
    <div>
      <Space wrap style={{ marginBottom: 16 }}>
        <RangePicker
          value={range}
          allowClear={false}
          onChange={(vals) => {
            if (vals?.[0] && vals?.[1]) {
              setRange([vals[0], vals[1]]);
              setPage(1);
            }
          }}
        />
        <Space>
          <Text type="secondary">仅统计练习范围内</Text>
          <Switch
            checked={inScopeOnly}
            onChange={(v) => {
              setInScopeOnly(v);
              setPage(1);
            }}
          />
        </Space>
        <Button
          icon={<DownloadOutlined />}
          loading={exporting}
          onClick={() => handleExport(false)}
        >
          导出当前区间
        </Button>
        <Button
          icon={<DownloadOutlined />}
          loading={exporting}
          onClick={() => handleExport(true)}
        >
          导出全部
        </Button>
      </Space>

      {summaryQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>
      ) : (
        <>
          {summary?.hint ? (
            <Alert type="info" showIcon style={{ marginBottom: 16 }} message={summary.hint} />
          ) : null}
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            <Col xs={12} sm={8} md={6}><Statistic title="在册学员" value={summary?.rosterCount ?? 0} /></Col>
            <Col xs={12} sm={8} md={6}><Statistic title="已开 App" value={summary?.withAccountCount ?? 0} /></Col>
            <Col xs={12} sm={8} md={6}><Statistic title="有刷题人数" value={summary?.activeUserCount ?? 0} /></Col>
            <Col xs={12} sm={8} md={6}>
              <Statistic title="参与率" value={fmtRate(summary?.participationRate)} />
            </Col>
            <Col xs={12} sm={8} md={6}><Statistic title="答题总数" value={summary?.answerCount ?? 0} /></Col>
            <Col xs={12} sm={8} md={6}>
              <Statistic title="正确率" value={fmtRate(summary?.accuracyRate)} />
            </Col>
            <Col xs={12} sm={8} md={6}>
              <Statistic title="人均答题" value={summary?.avgAnswersPerActive ?? 0} />
            </Col>
            <Col xs={12} sm={8} md={6}>
              <Statistic title="近7日活跃" value={summary?.activeUserCount7d ?? 0} />
            </Col>
          </Row>
        </>
      )}

      <Tabs
        items={[
          {
            key: 'students',
            label: '学员明细',
            children: (
              <>
                <Input.Search
                  allowClear
                  placeholder="姓名 / 手机 / 身份证"
                  style={{ width: 280, marginBottom: 12 }}
                  onSearch={(v) => {
                    setKeyword(v);
                    setPage(1);
                  }}
                />
                <Table
                  size="small"
                  rowKey="peopleId"
                  loading={studentsQuery.isLoading}
                  columns={studentColumns}
                  dataSource={studentsQuery.data?.items ?? []}
                  scroll={{ x: 1100 }}
                  pagination={{
                    current: page,
                    pageSize,
                    total: studentsQuery.data?.total ?? 0,
                    showSizeChanger: true,
                    onChange: (p, ps) => {
                      setPage(p);
                      setPageSize(ps);
                    },
                  }}
                />
              </>
            ),
          },
          {
            key: 'knowledges',
            label: '薄弱知识点',
            children: (
              <Table
                size="small"
                rowKey="knowledgeId"
                loading={knowledgesQuery.isLoading}
                columns={knowledgeColumns}
                dataSource={knowledgesQuery.data?.items ?? []}
                pagination={false}
                locale={{ emptyText: '暂无知识点刷题数据' }}
              />
            ),
          },
          {
            key: 'daily',
            label: '每日刷题',
            children: (
              <Table
                size="small"
                rowKey="bizDate"
                loading={dailyQuery.isLoading}
                columns={dailyColumns}
                dataSource={dailyQuery.data?.items ?? []}
                pagination={false}
                locale={{ emptyText: '所选日期内暂无刷题记录' }}
              />
            ),
          },
        ]}
      />
    </div>
  );
}

type ModalProps = {
  open: boolean;
  onClose: () => void;
  classId: ApiId | null;
  classTitle?: string;
};

export function ClassPracticeAnalyticsModal({ open, onClose, classId, classTitle }: ModalProps) {
  return (
    <Modal
      title={classTitle ? `刷题学情：${classTitle}` : '班级刷题学情'}
      open={open}
      onCancel={onClose}
      width={1080}
      footer={null}
      destroyOnHidden
    >
      <ClassPracticeAnalyticsPanel classId={classId} enabled={open} />
    </Modal>
  );
}
