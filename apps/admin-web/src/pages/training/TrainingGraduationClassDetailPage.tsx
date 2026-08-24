/**
 * 毕业典礼 · 班级详情（结业办理）
 * 隐藏路由：/training/graduation/classes/:classId
 */
import { useMemo, useState } from 'react';
import {
  Button,
  Card,
  Dropdown,
  Input,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  ArrowLeftOutlined,
  DownOutlined,
  DownloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { CertifiedRosterScope } from '../../api/training';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import { useGraduationStudentActions } from './GraduationStudentActions';

function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const ROSTER_SCOPE_MENU_ITEMS: MenuProps['items'] = [
  { key: 'certified', label: '已获证人员名册' },
  { key: 'all', label: '班级全部人员名册' },
];

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

async function downloadCertifiedRoster(
  classId: number,
  classTitle: string | undefined,
  scope: CertifiedRosterScope,
) {
  try {
    await trainApi.exportCertifiedRoster(classId, classTitle, scope);
  } catch (e) {
    message.error((e as Error).message || '导出失败');
  }
}

export function TrainingGraduationClassDetailPage({ classId }: { classId: number }) {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [certPreview, setCertPreview] = useState<FilePreviewTarget | null>(null);

  const { data: studentData, isLoading: studentLoading } = useQuery({
    queryKey: ['graduation-students', classId],
    queryFn: () => trainApi.listGraduationStudents(classId),
    enabled: classId > 0,
  });

  const invalidateClass = () => {
    void queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] });
    void queryClient.invalidateQueries({ queryKey: ['graduation-students', classId] });
  };

  const { actionColumn, modals: studentActionModals } = useGraduationStudentActions(classId, invalidateClass);

  const filteredStudents = useMemo(() => {
    const list = (studentData?.list ?? []) as Record<string, unknown>[];
    const q = keyword.trim().toLowerCase();
    if (!q) return list;
    return list.filter((r) => {
      const fields = [r.realname, r.phone, r.idCard, r.certificateNo, r.ticketNo];
      return fields.some((v) => String(v ?? '').toLowerCase().includes(q));
    });
  }, [studentData?.list, keyword]);

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
        title: '证书编号',
        width: 160,
        render: (_, r) => {
          if (!r.hasCertificate) {
            return <Typography.Text type="secondary">未建证</Typography.Text>;
          }
          const no = String(r.certificateNo || '已建证');
          const path = r.certificateFilePath ? String(r.certificateFilePath) : '';
          return path ? (
            <Button
              type="link"
              size="small"
              style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }}
              onClick={() =>
                setCertPreview({
                  fileUrl: path,
                  fileName: `${no}.pdf`,
                })
              }
            >
              {no}
            </Button>
          ) : (
            <Typography.Text>{no}</Typography.Text>
          );
        },
      },
      {
        title: '模板',
        dataIndex: 'templateTitle',
        width: 120,
        ellipsis: true,
        render: (v?: string) => v || '—',
      },
      actionColumn,
    ],
    [actionColumn],
  );

  const goBack = () => {
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    navigateTo('/training/graduation-ceremony');
  };

  return (
    <div className="training-page">
      <section className="page-heading" style={{ marginBottom: 12 }}>
        <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
          <div>
            <Button type="link" icon={<ArrowLeftOutlined />} onClick={goBack} style={{ paddingLeft: 0 }}>
              返回班级列表
            </Button>
            <Typography.Title level={4} style={{ margin: 0 }}>
              班级详情 · {studentData?.classTitle || `班级 ${classId}`}
            </Typography.Title>
            <Typography.Text type="secondary">
              班级状态 {classStatusTag(studentData?.graduateStatus)} · 成绩与考试模块一致（理论/技能/综合/工作业绩）；达标=理论与技能均≥60
            </Typography.Text>
          </div>
          <Dropdown
            menu={{
              items: ROSTER_SCOPE_MENU_ITEMS,
              onClick: ({ key }) =>
                void downloadCertifiedRoster(
                  classId,
                  studentData?.classTitle || undefined,
                  key as CertifiedRosterScope,
                ),
            }}
          >
            <Button icon={<DownloadOutlined />}>
              导出人员名册 <DownOutlined />
            </Button>
          </Dropdown>
        </Space>
      </section>

      <Card size="small" style={{ marginBottom: 12 }}>
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索姓名 / 手机 / 身份证 / 准考证号 / 证书编号"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ maxWidth: 420 }}
        />
      </Card>

      <Card size="small" title="学员名单">
        <Table
          rowKey="id"
          size="middle"
          loading={studentLoading}
          columns={studentColumns}
          dataSource={filteredStudents}
          pagination={{ pageSize: 20, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'] }}
          scroll={{ x: 1220 }}
        />
      </Card>

      {studentActionModals}
      <FilePreviewModal
        open={Boolean(certPreview)}
        target={certPreview}
        onClose={() => setCertPreview(null)}
      />
    </div>
  );
}
