/**
 * 分班 — 名单+排班自动建班（每班≤50）
 * 选规划 → 培训信息 → 一键自动分班 → 结果；手工勾选为调整入口
 */
import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Collapse,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckOutlined,
  DownloadOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import type {
  DivisionAutoPackResult,
  DivisionPlanItem,
  DivisionRosterItem,
} from '../../types/training';
import { useNavStore } from '../../stores/navStore';
import { TrainingProcessSteps } from './TrainingProcessSteps';

const { Text, Paragraph } = Typography;

export function TrainingClassDivisionPage() {
  const queryClient = useQueryClient();
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const now = new Date();

  const [step, setStep] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [readyOnly, setReadyOnly] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [selectedPlan, setSelectedPlan] = useState<DivisionPlanItem | null>(null);
  const [trainInfoId, setTrainInfoId] = useState<number | null>(null);
  const [trainInfoTitle, setTrainInfoTitle] = useState('');
  const [titlePrefix, setTitlePrefix] = useState('');
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [classMax, setClassMax] = useState(50);
  const [strategy, setStrategy] = useState<'by_shift_team' | 'by_roster_order'>('by_shift_team');
  const [useShiftFilter, setUseShiftFilter] = useState(true);

  const [packResult, setPackResult] = useState<DivisionAutoPackResult | null>(null);

  const [compatClassTitle, setCompatClassTitle] = useState('');
  const [selectedRosterIds, setSelectedRosterIds] = useState<number[]>([]);
  const [rosterPage, setRosterPage] = useState(1);

  const plansQuery = useQuery({
    queryKey: ['division-plans', page, pageSize, keyword, readyOnly],
    queryFn: () => trainApi.pageDivisionPlans({
      page,
      pageSize,
      keyword: keyword || undefined,
      readyOnly,
    }),
  });

  const readyQuery = useQuery({
    queryKey: ['division-ready', selectedPlan?.planId],
    queryFn: () => trainApi.getDivisionReady(selectedPlan!.planId),
    enabled: !!selectedPlan?.planId,
  });

  const rosterQuery = useQuery({
    queryKey: ['division-roster', selectedPlan?.planId, trainInfoId, rosterPage],
    queryFn: () => trainApi.pageDivisionRoster({
      planId: selectedPlan!.planId,
      trainInfoId: trainInfoId ?? undefined,
      assignStatus: 0,
      page: rosterPage,
      pageSize: 100,
    }),
    enabled: !!selectedPlan?.planId && !!trainInfoId && step >= 2,
  });

  const summaryQuery = useQuery({
    queryKey: ['division-summary', trainInfoId],
    queryFn: () => trainApi.getDivisionTrainInfoSummary(trainInfoId!),
    enabled: !!trainInfoId && step >= 3,
  });

  const ensureMu = useMutation({
    mutationFn: () => trainApi.ensureDivisionTrainInfo(selectedPlan!.planId, {
      title: titlePrefix || undefined,
      reuseExisting: true,
    }),
    onSuccess: (res) => {
      setTrainInfoId(res.trainInfoId);
      setTrainInfoTitle(res.title);
      message.success(res.created ? '已生成培训信息' : '已复用已有培训信息');
      queryClient.invalidateQueries({ queryKey: ['division-plans'] });
    },
    onError: (e: Error) => message.error(e.message || '生成培训信息失败'),
  });

  const packMu = useMutation({
    mutationFn: () => trainApi.autoPackDivision({
      planId: selectedPlan!.planId,
      trainInfoId: trainInfoId!,
      strategy,
      classMax,
      classNamePrefix: titlePrefix ? `${titlePrefix}-` : undefined,
      submitForCompanyConfirm: true,
      useShiftFilter,
      excludeNoIdCard: false,
      year,
      month,
      autoParseRoster: true,
    }),
    onSuccess: (res) => {
      setPackResult(res);
      setStep(3);
      message.success(`已自动创建 ${res.classCount} 个班，共分配 ${res.assignedCount} 人`);
      queryClient.invalidateQueries({ queryKey: ['division-summary'] });
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['enterprise-class-confirm'] });
    },
    onError: (e: Error) => message.error(e.message || '自动分班失败'),
  });

  const parseMu = useMutation({
    mutationFn: () => trainApi.parseDivisionRoster({
      planId: selectedPlan!.planId,
      trainInfoId: trainInfoId ?? undefined,
      mode: 'enterprise_roster',
      fallbackToPlanItem: true,
    }),
    onSuccess: (res) => {
      message.success(`已解析 ${res.total} 人`);
      setSelectedRosterIds([]);
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
    },
    onError: (e: Error) => message.error(e.message || '解析名单失败'),
  });

  const splitMu = useMutation({
    mutationFn: () => trainApi.submitDivisionOrgSplit({
      planId: selectedPlan!.planId,
      trainInfoId: trainInfoId!,
      submitForCompanyConfirm: true,
      classes: [{
        title: compatClassTitle.trim() || `${selectedPlan?.orgName || '培训'}-1班`,
        rosterIds: selectedRosterIds,
      }],
    }),
    onSuccess: (res) => {
      message.success(`手工分班成功：${res.classCount} 个班级`);
      setSelectedRosterIds([]);
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['enterprise-class-confirm'] });
    },
    onError: (e: Error) => message.error(e.message || '分班提交失败'),
  });

  const materialsOk = !!(
    selectedPlan
    && (selectedPlan.rosterFileId || readyQuery.data?.rosterFileId)
    && (selectedPlan.shiftFileId || readyQuery.data?.shiftFileId)
  );
  const missing = readyQuery.data?.missing ?? [];

  const planColumns: ColumnsType<DivisionPlanItem> = [
    { title: '规划编号', dataIndex: 'planNo', width: 160 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '企业', dataIndex: 'orgName', width: 160 },
    {
      title: '材料',
      width: 140,
      render: (_, row) => (
        <Space size={4}>
          <Tag color={row.rosterFileId ? 'success' : 'error'}>名单</Tag>
          <Tag color={row.shiftFileId ? 'success' : 'error'}>排班</Tag>
        </Space>
      ),
    },
    {
      title: '操作',
      width: 100,
      render: (_, row) => (
        <Button
          type="link"
          size="small"
          disabled={!row.rosterFileId || !row.shiftFileId}
          onClick={() => {
            setSelectedPlan(row);
            setTrainInfoId(null);
            setTrainInfoTitle('');
            setTitlePrefix(row.orgName || row.title);
            setCompatClassTitle(`${row.orgName || row.title}-培训班`);
            setPackResult(null);
            setSelectedRosterIds([]);
            setStep(1);
          }}
        >
          分班
        </Button>
      ),
    },
  ];

  const rosterColumns: ColumnsType<DivisionRosterItem> = [
    { title: '姓名', dataIndex: 'personName', width: 100 },
    { title: '身份证', dataIndex: 'idCard', width: 180 },
    { title: '班组', dataIndex: 'shiftTeam', width: 100 },
    { title: '工种', dataIndex: 'jobName', width: 120 },
  ];

  return (
    <div className="training-page">
      <TrainingProcessSteps current={3} />

      <section className="page-heading">
        <Typography.Title level={4}>分班</Typography.Title>
        <Text type="secondary">
          系统根据人员名单与排班表自动划分培训班，每班不超过 {classMax} 人
        </Text>
      </section>

      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Steps
          current={step}
          items={[
            { title: '选择规划' },
            { title: '培训信息' },
            { title: '自动分班' },
            { title: '结果' },
          ]}
          onChange={(s) => {
            if (s === 0) { setStep(0); return; }
            if (!selectedPlan) { message.warning('请先选择规划'); return; }
            if (s >= 1 && !materialsOk) {
              message.warning('需同时具备筛选名单与排班表');
              return;
            }
            if (s >= 2 && !trainInfoId) {
              message.warning('请先生成培训信息');
              return;
            }
            if (s >= 3 && !packResult) {
              message.warning('请先执行自动分班');
              return;
            }
            setStep(s);
          }}
        />
      </Card>

      {step === 0 && (
        <Card bordered={false}>
          <Space style={{ marginBottom: 16 }} wrap>
            <Input.Search
              allowClear
              placeholder="搜索规划标题/编号"
              style={{ width: 260 }}
              onSearch={(v) => { setKeyword(v); setPage(1); }}
            />
            <Checkbox
              checked={readyOnly}
              onChange={(e) => { setReadyOnly(e.target.checked); setPage(1); }}
            >
              仅显示材料已齐
            </Checkbox>
            <Button icon={<ReloadOutlined />} onClick={() => plansQuery.refetch()}>刷新</Button>
          </Space>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message="按生产班组/倒班类型分组后，每组按人数上限切开成培训班（默认每班不超过50人），再提交企业确认。"
          />
          <Table<DivisionPlanItem>
            rowKey="planId"
            loading={plansQuery.isLoading}
            columns={planColumns}
            dataSource={plansQuery.data?.items ?? []}
            pagination={{
              current: page,
              pageSize,
              total: plansQuery.data?.total ?? 0,
              onChange: (p, ps) => { setPage(p); setPageSize(ps); },
            }}
          />
        </Card>
      )}

      {step === 1 && selectedPlan && (
        <Card bordered={false}>
          {!materialsOk && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={`材料未齐：${missing.join('、') || '筛选名单/排班表'}`}
            />
          )}
          <Paragraph>
            <Text strong>规划：</Text>{selectedPlan.planNo} · {selectedPlan.title}
            <br />
            <Text strong>企业：</Text>{selectedPlan.orgName || '-'}
          </Paragraph>
          <Space style={{ marginBottom: 16 }} wrap>
            <Button
              icon={<DownloadOutlined />}
              disabled={!selectedPlan.rosterFileId}
              onClick={() => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-roster')}
            >
              下载筛选名单
            </Button>
            <Button
              icon={<DownloadOutlined />}
              disabled={!selectedPlan.shiftFileId}
              onClick={() => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-shift')}
            >
              下载排班表
            </Button>
          </Space>
          <Form layout="vertical" style={{ maxWidth: 520 }}>
            <Form.Item label="班级名前缀">
              <Input
                value={titlePrefix}
                onChange={(e) => setTitlePrefix(e.target.value)}
                placeholder="如：来铝 → 来铝-一班-1班"
              />
            </Form.Item>
            <Space wrap size="large">
              <Form.Item label="排班年" style={{ marginBottom: 0 }}>
                <InputNumber min={2020} max={2100} value={year} onChange={(v) => setYear(v || now.getFullYear())} />
              </Form.Item>
              <Form.Item label="排班月" style={{ marginBottom: 0 }}>
                <InputNumber min={1} max={12} value={month} onChange={(v) => setMonth(v || 1)} />
              </Form.Item>
              <Form.Item label="每班上限" style={{ marginBottom: 0 }}>
                <InputNumber min={1} max={200} value={classMax} onChange={(v) => setClassMax(v || 50)} />
              </Form.Item>
            </Space>
          </Form>
          {trainInfoId && (
            <Alert
              type="success"
              showIcon
              style={{ margin: '16px 0' }}
              message={`培训信息 #${trainInfoId}：${trainInfoTitle}`}
            />
          )}
          <Space style={{ marginTop: 16 }}>
            <Button onClick={() => setStep(0)}>上一步</Button>
            <Button
              type="primary"
              loading={ensureMu.isPending}
              disabled={!materialsOk}
              onClick={() => ensureMu.mutate()}
            >
              {trainInfoId ? '确认培训信息' : '生成/复用培训信息'}
            </Button>
            <Button type="primary" ghost disabled={!trainInfoId} onClick={() => setStep(2)}>
              下一步：自动分班
            </Button>
          </Space>
        </Card>
      )}

      {step === 2 && selectedPlan && trainInfoId && (
        <Card bordered={false}>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message={`将解析名单（若尚未入池），结合 ${year}年${month}月 排班归类后按每班≤${classMax} 人自动建班。`}
          />
          <Form layout="inline" style={{ marginBottom: 16 }}>
            <Form.Item label="策略">
              <Select
                style={{ width: 200 }}
                value={strategy}
                onChange={setStrategy}
                options={[
                  { value: 'by_shift_team', label: '按班组/倒班（推荐）' },
                  { value: 'by_roster_order', label: '按名单顺序切班' },
                ]}
              />
            </Form.Item>
            <Form.Item>
              <Checkbox checked={useShiftFilter} onChange={(e) => setUseShiftFilter(e.target.checked)}>
                过滤当月无可上课日人员
              </Checkbox>
            </Form.Item>
          </Form>
          <Space style={{ marginBottom: 16 }} wrap>
            <Button
              type="primary"
              loading={packMu.isPending}
              onClick={() => {
                Modal.confirm({
                  title: '确认自动分班？',
                  content: `策略：${strategy === 'by_shift_team' ? '按班组' : '名单顺序'}；每班不超过 ${classMax} 人。将创建培训班并写入学员，提交企业确认。`,
                  onOk: () => packMu.mutateAsync(),
                });
              }}
            >
              一键自动分班
            </Button>
            <Button onClick={() => setStep(1)}>上一步</Button>
          </Space>

          <Collapse
            items={[
              {
                key: 'manual',
                label: '手工调整：勾选名单分班',
                children: (
                  <div>
                    <Space style={{ marginBottom: 12 }} wrap>
                      <Button loading={parseMu.isPending} onClick={() => parseMu.mutate()}>
                        解析确认名单
                      </Button>
                      <Input
                        style={{ width: 240 }}
                        value={compatClassTitle}
                        onChange={(e) => setCompatClassTitle(e.target.value)}
                        placeholder="班级名称"
                      />
                      <Button
                        type="primary"
                        icon={<CheckOutlined />}
                        loading={splitMu.isPending}
                        disabled={selectedRosterIds.length === 0 || !compatClassTitle.trim()}
                        onClick={() => {
                          Modal.confirm({
                            title: '手工提交分班？',
                            onOk: () => splitMu.mutateAsync(),
                          });
                        }}
                      >
                        保存并提交企业确认
                      </Button>
                    </Space>
                    <Table<DivisionRosterItem>
                      rowKey="id"
                      size="small"
                      loading={rosterQuery.isLoading}
                      columns={rosterColumns}
                      dataSource={rosterQuery.data?.items ?? []}
                      rowSelection={{
                        selectedRowKeys: selectedRosterIds,
                        onChange: (keys) => setSelectedRosterIds(keys as number[]),
                      }}
                      pagination={{
                        current: rosterPage,
                        pageSize: 100,
                        total: rosterQuery.data?.total ?? 0,
                        onChange: (p) => setRosterPage(p),
                      }}
                    />
                  </div>
                ),
              },
            ]}
          />
        </Card>
      )}

      {step === 3 && selectedPlan && trainInfoId && packResult && (
        <Card bordered={false}>
          <Space style={{ marginBottom: 16 }} wrap>
            <Tag color="success" icon={<TeamOutlined />}>
              {packResult.classCount} 个班 · {packResult.assignedCount} 人
            </Tag>
            <Tag>策略 {packResult.strategy}</Tag>
            <Tag color="orange">未分入 {packResult.unassignedCount}</Tag>
            <Tag color="blue">
              汇总 班级 {summaryQuery.data?.classCount ?? '-'} / 学员 {summaryQuery.data?.peopleCount ?? '-'}
            </Tag>
          </Space>
          <Alert
            type="success"
            showIcon
            style={{ marginBottom: 16 }}
            message="已创建培训班并进入待企业确认。可前往企业分班确认或班级人员查看。"
          />
          <Table
            rowKey="classId"
            size="small"
            pagination={false}
            style={{ marginBottom: 16 }}
            dataSource={packResult.classes}
            columns={[
              { title: '班级名称', dataIndex: 'title' },
              { title: '分组', dataIndex: 'group', width: 120 },
              { title: '班级ID', dataIndex: 'classId', width: 100 },
              { title: '人数', dataIndex: 'nums', width: 80 },
            ]}
          />
          {(packResult.unassigned?.length ?? 0) > 0 && (
            <Table
              rowKey={(r) => `${r.rosterId}-${r.reason}`}
              size="small"
              style={{ marginBottom: 16 }}
              pagination={{ pageSize: 10 }}
              dataSource={packResult.unassigned}
              columns={[
                { title: '未分入人员', dataIndex: 'personName', width: 100 },
                { title: '身份证', dataIndex: 'idCard', width: 180 },
                { title: '原因', dataIndex: 'reason' },
              ]}
            />
          )}
          {(packResult.warnings?.length ?? 0) > 0 && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={`警告 ${packResult.warnings!.length} 条`}
              description={packResult.warnings!.slice(0, 8).map((w) => w.message).join('；')}
            />
          )}
          <Space>
            <Button onClick={() => setStep(2)}>返回</Button>
            <Button type="primary" onClick={() => navigateTo('train_people-list')}>
              去班级人员
            </Button>
            <Button onClick={() => navigateTo('company_plan_review')}>
              去企业分班确认
            </Button>
            <Button onClick={() => { setStep(0); setPackResult(null); }}>
              处理下一规划
            </Button>
          </Space>
        </Card>
      )}
    </div>
  );
}
