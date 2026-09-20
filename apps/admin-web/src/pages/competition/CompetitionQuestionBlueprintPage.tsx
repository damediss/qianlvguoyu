import { ApartmentOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Modal, Select, Space, Table, Tag, Tree, Upload, message } from 'antd';
import type { UploadProps } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { useEffect, useState } from 'react';
import {
  activateCompetitionQuestionBlueprint,
  archiveCompetitionQuestionBlueprint,
  downloadCompetitionQuestionBlueprintSource,
  downloadCompetitionQuestionBlueprintTemplate,
  getCompetitionQuestionBlueprintCoverage,
  getCompetitionQuestionBlueprintTree,
  importCompetitionQuestionBlueprint,
  pageCompetitionQuestionBlueprints,
} from '../../api/competitionQuestionBlueprint';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents } from '../../api/competition';
import type { CompetitionEvent, CompetitionEventWorkType, CompetitionEventWorkTypeLevel } from '../../types/competition';
import type { CompetitionQuestionContentType } from '../../types/competitionQuestion';
import type { CompetitionQuestionBlueprint } from '../../types/competitionQuestionBlueprint';

interface Props { contentType: CompetitionQuestionContentType; }

function eventFromUrl() { return new URLSearchParams(window.location.search).get('eventId') || ''; }

type ApiErrorLike = {
  message?: unknown;
  response?: { data?: unknown };
};

export function getCompetitionQuestionBlueprintErrorMessage(error: unknown): string {
  const candidate = error && typeof error === 'object' ? error as ApiErrorLike : {};
  const responseData = candidate.response?.data;
  if (responseData && typeof responseData === 'object' && 'message' in responseData) {
    const responseMessage = (responseData as { message?: unknown }).message;
    if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
  }
  if (typeof responseData === 'string' && responseData.trim()) return responseData;
  if (typeof candidate.message === 'string' && candidate.message.trim()) return candidate.message;
  return '请求失败';
}

function checkReportLines(report?: string | null) {
  if (!report) return [];
  try {
    const parsed = JSON.parse(report) as { errors?: string[]; warnings?: string[] };
    return [
      ...(parsed.errors || []).map((item) => `错误：${item}`),
      ...(parsed.warnings || []).map((item) => `警告：${item}`),
    ];
  } catch {
    return [report];
  }
}

export function CompetitionQuestionBlueprintPage({ contentType }: Props) {
  const queryClient = useQueryClient();
  const [eventId, setEventId] = useState(eventFromUrl);
  const [workTypeId, setWorkTypeId] = useState('');
  const [levelId, setLevelId] = useState('');
  const [treeId, setTreeId] = useState<string | null>(null);
  const [warningUpload, setWarningUpload] = useState<File | null>(null);
  const [warningMessage, setWarningMessage] = useState<string | null>(null);
  const eventsQuery = useQuery({ queryKey: ['competition-events', 'blueprints'], queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }) });
  const workTypesQuery = useQuery({ queryKey: ['competition-blueprints-work-types', eventId], queryFn: () => listCompetitionEventWorkTypes(eventId), enabled: Boolean(eventId) });
  const levelsQuery = useQuery({ queryKey: ['competition-blueprints-levels', eventId, workTypeId], queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId), enabled: Boolean(eventId && workTypeId) });
  const pageQuery = useQuery({
    queryKey: ['competition-blueprints', eventId, workTypeId, levelId, contentType],
    queryFn: () => pageCompetitionQuestionBlueprints(eventId, { page: 1, pageSize: 100, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType }),
    enabled: Boolean(eventId && workTypeId && levelId),
  });
  const treeQuery = useQuery({ queryKey: ['competition-blueprint-tree', eventId, treeId], queryFn: () => getCompetitionQuestionBlueprintTree(eventId, treeId!), enabled: Boolean(eventId && treeId) });
  const coverageQuery = useQuery({ queryKey: ['competition-blueprint-coverage', eventId, treeId], queryFn: () => getCompetitionQuestionBlueprintCoverage(eventId, treeId!), enabled: Boolean(eventId && treeId) });
  const importMutation = useMutation({
    mutationFn: (payload: { file: File; confirmWarnings: boolean }) => importCompetitionQuestionBlueprint(eventId, { eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, confirmWarnings: payload.confirmWarnings }, payload.file),
    onSuccess: () => { message.success('细目表已导入为草稿版本，请检查后启用'); queryClient.invalidateQueries({ queryKey: ['competition-blueprints'] }); setWarningUpload(null); setWarningMessage(null); },
    onError: (error: unknown) => {
      // The first request is deliberately sent without confirmation so the
      // server can return the exact title/metadata/XYZ warning. Keep the file
      // for the explicit second confirmation; close only hard validation errors.
      const errorMessage = getCompetitionQuestionBlueprintErrorMessage(error);
      if (errorMessage.includes('待确认警告')) {
        setWarningMessage(errorMessage);
      } else {
        setWarningUpload(null);
        setWarningMessage(null);
      }
      if (!errorMessage.includes('待确认警告')) message.error(errorMessage || '细目表导入失败');
    },
  });
  const activateMutation = useMutation({ mutationFn: (id: string) => activateCompetitionQuestionBlueprint(eventId, id, true), onSuccess: () => { message.success('细目表已启用'); queryClient.invalidateQueries({ queryKey: ['competition-blueprints'] }); } });
  const archiveMutation = useMutation({ mutationFn: (id: string) => archiveCompetitionQuestionBlueprint(eventId, id), onSuccess: () => { message.success('细目表已归档'); queryClient.invalidateQueries({ queryKey: ['competition-blueprints'] }); } });
  const events = eventsQuery.data?.items ?? [];
  const workTypes = workTypesQuery.data ?? [];
  const levels = levelsQuery.data ?? [];
  useEffect(() => { if (!eventId && events[0]) setEventId(String(events[0].id)); }, [eventId, events]);
  useEffect(() => { if (!workTypeId && workTypes[0]) setWorkTypeId(String(workTypes[0].id)); }, [workTypeId, workTypes]);
  useEffect(() => { if (!levelId && levels[0]) setLevelId(String(levels[0].id)); }, [levelId, levels]);

  const uploadProps: UploadProps = {
    accept: '.xlsx,.xls', showUploadList: false,
    beforeUpload: (file) => {
      setWarningUpload(file);
      setWarningMessage(null);
      return Upload.LIST_IGNORE;
    },
  };
  const uploadConfirmed = () => {
    if (warningUpload) {
      importMutation.mutate({ file: warningUpload, confirmWarnings: Boolean(warningMessage) });
    }
  };
  const downloadSource = async (row: CompetitionQuestionBlueprint) => {
    const blob = await downloadCompetitionQuestionBlueprintSource(eventId, row.id);
    const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = row.sourceFileName || '细目表.xlsx'; a.click(); URL.revokeObjectURL(url);
  };
  const downloadTemplate = async () => {
    try {
      const blob = await downloadCompetitionQuestionBlueprintTemplate(contentType);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = contentType === 'theory' ? '竞赛理论细目表导入模板.xlsx' : '竞赛实操细目表导入模板.xlsx';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '导入模板下载失败');
    }
  };
  const activateRow = (row: CompetitionQuestionBlueprint) => {
    const warnings = checkReportLines(row.checkReport).filter((line) => line.startsWith('警告：'));
    if (warnings.length === 0) {
      activateMutation.mutate(row.id);
      return;
    }
    Modal.confirm({
      title: '确认启用带警告的细目表？',
      content: <Space direction="vertical">{warnings.map((warning, index) => <span key={`${index}-${warning}`}>{warning}</span>)}</Space>,
      okText: '确认启用',
      cancelText: '返回检查',
      onOk: () => activateMutation.mutate(row.id),
    });
  };
  const toTreeNode = (node: NonNullable<typeof treeQuery.data>['tree'][number]): DataNode => ({
    key: node.id,
    title: `${node.fullCode} ${node.name}`,
    children: node.children?.map(toTreeNode),
  });
  const treeData = treeQuery.data?.tree.map(toTreeNode);
  const reportLines = checkReportLines(treeQuery.data?.blueprint.checkReport);
  return <Card title={`竞赛${contentType === 'theory' ? '理论' : '实操'}细目表`} extra={<Space wrap>
    <Select showSearch optionFilterProp="label" style={{ width: 240 }} placeholder="选择赛事" value={eventId || undefined} options={events.map((e: CompetitionEvent) => ({ value: String(e.id), label: `${e.name}（${e.code}）` }))} onChange={(v) => { setEventId(v); setWorkTypeId(''); setLevelId(''); }} />
    <Select style={{ width: 210 }} placeholder="选择工种" value={workTypeId || undefined} options={workTypes.map((w: CompetitionEventWorkType) => ({ value: String(w.id), label: w.displayName || w.workTypeName || String(w.workTypeId) }))} onChange={(v) => { setWorkTypeId(v); setLevelId(''); }} />
    <Select style={{ width: 170 }} placeholder="选择等级" value={levelId || undefined} options={levels.map((l: CompetitionEventWorkTypeLevel) => ({ value: String(l.id), label: `${l.levelName}（${l.levelCode}）` }))} onChange={setLevelId} />
    <Button icon={<DownloadOutlined />} onClick={() => void downloadTemplate()}>下载导入模板</Button>
    <Upload {...uploadProps} disabled={!eventId || !workTypeId || !levelId || importMutation.isPending}><Button type="primary" icon={<UploadOutlined />}>导入细目表</Button></Upload>
  </Space>}>
    <Alert type="info" showIcon message="细目表版本先导入为草稿；启用后，题库导入和自动组卷只能使用该版本的末级考评点。标题、元数据或 XYZ 比例警告需人工确认。" style={{ marginBottom: 16 }} />
    <Table<CompetitionQuestionBlueprint> rowKey="id" loading={pageQuery.isLoading} dataSource={pageQuery.data?.items ?? []} pagination={false} scroll={{ x: 'max-content' }} columns={[
      { title: '版本', dataIndex: 'versionNo', width: 80, render: (v: number) => `v${v}` },
      { title: '标题', dataIndex: 'title', ellipsis: true },
      { title: '主题/考评点', key: 'counts', render: (_: unknown, row: CompetitionQuestionBlueprint) => `${row.topicCount} / ${row.pointCount}` },
      { title: 'XYZ', dataIndex: 'xyzRatio' },
      { title: '状态', dataIndex: 'status', render: (v: string) => <Tag color={v === 'active' ? 'green' : v === 'draft' ? 'gold' : 'default'}>{v === 'active' ? '启用' : v === 'draft' ? '草稿' : '已归档'}</Tag> },
      { title: '操作', key: 'actions', render: (_: unknown, row: CompetitionQuestionBlueprint) => <Space><Button type="link" icon={<ApartmentOutlined />} onClick={() => setTreeId(row.id)}>树形预览</Button><Button type="link" icon={<DownloadOutlined />} onClick={() => void downloadSource(row)}>源文件</Button>{row.status === 'draft' && <Button type="link" onClick={() => activateRow(row)}>启用</Button>}{row.status === 'draft' && <Button type="link" danger onClick={() => archiveMutation.mutate(row.id)}>归档</Button>}</Space> },
    ]} />
    <Modal
      title={warningMessage ? '确认细目表导入警告' : '校验细目表'}
      open={Boolean(warningUpload)}
      onCancel={() => { setWarningUpload(null); setWarningMessage(null); }}
      onOk={uploadConfirmed}
      okText={warningMessage ? '确认警告并导入' : '校验并导入'}
      confirmLoading={importMutation.isPending}
    >
      <Alert
        type={warningMessage ? 'warning' : 'info'}
        showIcon
        message={warningMessage || '系统会先执行层级、权重、末级考评点和元数据校验；如发现警告，会在下一步展示具体内容。'}
      />
    </Modal>
    <Modal title="细目表树形预览" open={Boolean(treeId)} onCancel={() => setTreeId(null)} footer={null} width={980}>{treeQuery.data && <>
      <Descriptions size="small" column={5}><Descriptions.Item label="版本">v{treeQuery.data.blueprint.versionNo}</Descriptions.Item><Descriptions.Item label="考评点">{treeQuery.data.blueprint.pointCount}</Descriptions.Item><Descriptions.Item label="主题">{treeQuery.data.blueprint.topicCount}</Descriptions.Item><Descriptions.Item label="已覆盖考评点">{coverageQuery.data?.coveredPoints ?? '-'}</Descriptions.Item><Descriptions.Item label="已绑定题目">{coverageQuery.data?.boundQuestions ?? '-'}</Descriptions.Item></Descriptions>
      {reportLines.length > 0 && <Alert type={reportLines.some((line) => line.startsWith('错误：')) ? 'error' : 'warning'} showIcon message="导入校验报告" description={<Space direction="vertical">{reportLines.map((line, index) => <span key={`${index}-${line}`}>{line}</span>)}</Space>} style={{ marginBottom: 12 }} />}
      <Tree showLine treeData={treeData} /><Table size="small" style={{ marginTop: 16 }} rowKey="nodeId" loading={coverageQuery.isLoading} dataSource={coverageQuery.data?.points ?? []} pagination={{ pageSize: 8 }} columns={[{ title: '考评点', dataIndex: 'path', ellipsis: true }, { title: '编码', dataIndex: 'fullCode', width: 140 }, { title: '题量要求', dataIndex: 'questionQuota', width: 90 }, { title: '已绑定', dataIndex: 'boundQuestionCount', width: 90 }, { title: '缺口', dataIndex: 'shortage', width: 80, render: (value: number) => value > 0 ? <Tag color="red">{value}</Tag> : <Tag color="green">0</Tag> }]} /></>}</Modal>
  </Card>;
}
