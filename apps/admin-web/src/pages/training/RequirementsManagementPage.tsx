/**
 * 管理端需求管理 — 对齐 yun_shang requirementsManagement.vue
 * 待办：企业原始需求 → 处理立项
 * 已办：train_info_list 列表（筛选 + 手动添加立项）
 */
import { useMemo, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Select,
  Table,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type {
  DemandConvertReq,
  DemandPending,
  DemandPublicData,
  TrainInfoListItem,
  TrainInfoListQuery,
} from '../../types/training';
import './RequirementsManagementPage.css';

const WORK_LEVELS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

const TRAIN_COMPANY_OPTIONS = [
  { value: '千铝工投', label: '千铝工投' },
  { value: '广西现代职业技术学院', label: '广西现代职业技术学院' },
  { value: '广西工业技师学院', label: '广西工业技师学院' },
];

function formatWorkTypeTitle(item: TrainInfoListItem): string {
  const title = item.work_type_title ?? '-';
  const level = item.work_type_level;
  const levelText = level && level > 0 ? `${level}级` : '-';
  return `${title}(${levelText})`;
}

export function RequirementsManagementPage() {
  const queryClient = useQueryClient();
  /** 0=待办, -1=已办（与 Vue headConfirmSwitch 一致） */
  const [headConfirmSwitch, setHeadConfirmSwitch] = useState(0);
  const [convertForm] = Form.useForm<DemandConvertReq>();
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const pendingQuery = { page: 1, pageSize: 30 };

  const infoListQuery: TrainInfoListQuery = useMemo(
    () => ({
      page: currentPage,
      pageSize,
      passStatus: 1,
      headConfirm: headConfirmSwitch,
      companyId: companyIdFilter,
      workTypeId: workTypeIdFilter,
      groupTypeNe: 1,
    }),
    [currentPage, pageSize, headConfirmSwitch, companyIdFilter, workTypeIdFilter],
  );

  const { data: publicData } = useQuery({
    queryKey: ['demand-public-data', 'requirements'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
    staleTime: 5 * 60 * 1000,
  });

  const { data: pendings, isLoading: pendingsLoading } = useQuery({
    queryKey: ['demand-pendings', pendingQuery],
    queryFn: () => trainApi.pageDemandPendings(pendingQuery),
    enabled: headConfirmSwitch === 0,
  });

  const { data: infoList, isLoading: infoListLoading } = useQuery({
    queryKey: ['training-info-requirements', infoListQuery],
    queryFn: () => trainApi.pageInfoLists(infoListQuery),
    enabled: headConfirmSwitch === -1,
  });

  const companyOptions =
    (publicData as DemandPublicData | undefined)?.company_list?.map((c) => ({
      value: c.id,
      label: c.title,
    })) ?? [];

  const workTypeGroupedOptions =
    (publicData as DemandPublicData | undefined)?.worktype_list?.map((g) => ({
      label: g.title,
      options: (g.options ?? []).map((o) => ({ value: o.id, label: o.title })),
    })) ?? [];

  const resetConvertForm = () => {
    convertForm.resetFields();
    convertForm.setFieldsValue({
      trainCompanyName: '千铝工投',
      num: 0,
      traindemandId: 0,
    });
  };

  const handleSwitch = () => {
    const next = headConfirmSwitch === 0 ? -1 : 0;
    setHeadConfirmSwitch(next);
    if (next === -1) {
      setCurrentPage(1);
    }
  };

  const openConvertModal = async (prefill?: Partial<DemandConvertReq>) => {
    await queryClient.ensureQueryData({
      queryKey: ['demand-public-data', 'requirements'],
      queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
    });
    resetConvertForm();
    if (prefill) {
      convertForm.setFieldsValue({
        trainCompanyName: '千铝工投',
        ...prefill,
      });
    }
    setModalOpen(true);
  };

  const handleDemand = (row: DemandPending) => {
    openConvertModal({
      companyId: row.orgId,
      num: row.nums,
      traindemandId: row.id,
    });
  };

  const handleTrainDemand = () => {
    openConvertModal();
  };

  const handleTrainDemandClose = () => {
    setModalOpen(false);
    resetConvertForm();
  };

  const submitTrainDemand = async () => {
    try {
      const values = await convertForm.validateFields();
      if (submitting) return;
      setSubmitting(true);
      await trainApi.convertDemandToTraining({
        ...values,
        traindemandId: values.traindemandId || undefined,
      });
      message.success('提交成功');
      handleTrainDemandClose();
      await queryClient.invalidateQueries({ queryKey: ['demand-pendings'] });
      await queryClient.invalidateQueries({ queryKey: ['training-info-requirements'] });
      if (headConfirmSwitch === 0) {
        setHeadConfirmSwitch(-1);
        setCurrentPage(1);
      }
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) {
        return;
      }
      message.error('提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setCompanyIdFilter(undefined);
    setWorkTypeIdFilter(undefined);
    setCurrentPage(1);
  };

  const pendingColumns: ColumnsType<DemandPending> = [
    { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
    { title: '接受培训单位', dataIndex: 'company', width: 300, align: 'center' },
    { title: '单位类型', dataIndex: 'companyType', width: 150, align: 'center' },
    { title: '内容', dataIndex: 'content', align: 'center' },
    {
      title: '发布人',
      dataIndex: 'publisher',
      width: 200,
      align: 'center',
      ellipsis: true,
      render: (v: string | undefined, row) => v || row.company || '-',
    },
    { title: '人数', dataIndex: 'nums', width: 200, align: 'center' },
    { title: '创建时间', dataIndex: 'addtimeCn', width: 200, align: 'center' },
    {
      title: '操作',
      key: 'act',
      width: 80,
      fixed: 'right',
      align: 'center',
      render: (_, row) => (
        <Button type="primary" size="small" onClick={() => handleDemand(row)}>
          处理
        </Button>
      ),
    },
  ];

  const doneColumns: ColumnsType<TrainInfoListItem> = [
    { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
    { title: '标题', dataIndex: 'title', align: 'center', width: 200 },
    { title: '接受培训公司', dataIndex: 'company_name', align: 'center' },
    { title: '培训机构', dataIndex: 'train_company_name', align: 'center', width: 150 },
    {
      title: '工种',
      key: 'workTypeTitle',
      align: 'center',
      width: 150,
      render: (_, row) => formatWorkTypeTitle(row),
    },
    { title: '人数', dataIndex: 'num', width: 60, align: 'center' },
    {
      title: '是否立项',
      dataIndex: 'pass_status',
      width: 100,
      align: 'center',
      render: (v: number) => (v === 0 ? '不立项' : '立项'),
    },
    { title: '创建时间', dataIndex: 'create_time', align: 'center', width: 174 },
    { title: '操作', key: 'act', width: 60, fixed: 'right', align: 'center', render: () => null },
  ];

  return (
    <div className="requirements-management-page training-page training-table-container">
      <section className="page-heading">
        <Typography.Title level={4}>需求管理</Typography.Title>
        <Typography.Text type="secondary">处理企业培训需求，跟踪立项与培训项目进度</Typography.Text>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
      <div className="search-filter-bar">
        <div className="filter-actions">
          <Button type={headConfirmSwitch === 0 ? 'primary' : 'default'} onClick={handleSwitch}>
            待办
          </Button>
          <Button
            className="tab-btn-spacer"
            type={headConfirmSwitch === -1 ? 'primary' : 'default'}
            onClick={handleSwitch}
          >
            已办
          </Button>
          {headConfirmSwitch === -1 && (
            <>
              <Select
                className="filter-select"
                value={companyIdFilter}
                onChange={(v) => {
                  setCompanyIdFilter(v);
                  setCurrentPage(1);
                }}
                placeholder="企业筛选"
                allowClear
                showSearch
                optionFilterProp="label"
                options={companyOptions}
              />
              <Select
                className="filter-select"
                value={workTypeIdFilter}
                onChange={(v) => {
                  setWorkTypeIdFilter(v);
                  setCurrentPage(1);
                }}
                placeholder="请选择工种"
                allowClear
                showSearch
                optionFilterProp="label"
                options={workTypeGroupedOptions}
              />
              <Button type="primary" onClick={handleReset}>
                重置
              </Button>
              <Button type="primary" onClick={handleTrainDemand}>
                添加
              </Button>
            </>
          )}
        </div>
      </div>
      </Card>

      {headConfirmSwitch === 0 && (
        <Table
          className="requirements-table"
          rowKey="id"
          bordered
          size="middle"
          loading={pendingsLoading}
          columns={pendingColumns}
          dataSource={pendings?.items ?? []}
          scroll={{ x: 900, y: 600 }}
          pagination={false}
        />
      )}

      {headConfirmSwitch === -1 && (
        <>
          <Table
            className="requirements-table"
            rowKey="id"
            bordered
            size="middle"
            loading={infoListLoading}
            columns={doneColumns}
            dataSource={infoList?.list ?? []}
            scroll={{ x: 1000, y: 600 }}
            pagination={false}
          />
          <Pagination
            className="requirements-pagination"
            current={currentPage}
            pageSize={pageSize}
            total={infoList?.total ?? 0}
            showSizeChanger
            pageSizeOptions={['10', '20', '50', '100']}
            showQuickJumper
            showTotal={(t) => `共 ${t} 条`}
            onChange={(p, ps) => {
              if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                setPageSize(ps);
                setCurrentPage(1);
              } else {
                setCurrentPage(p);
              }
            }}
          />
        </>
      )}

      <Modal
        title="培训需求管理"
        open={modalOpen}
        onOk={submitTrainDemand}
        onCancel={handleTrainDemandClose}
        confirmLoading={submitting}
        destroyOnHidden
        width="50%"
        okText="确 定"
        cancelText="取 消"
      >
        <Form form={convertForm} className="convert-form" labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入标题" />
          </Form.Item>
          <Form.Item
            name="companyId"
            label="接受培训的单位"
            rules={[{ required: true, message: '请选择接受培训的单位' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="请选择接受培训的单位"
              options={companyOptions}
            />
          </Form.Item>
          <Form.Item name="trainCompanyName" label="培训机构">
            <Select placeholder="请选择培训机构" options={TRAIN_COMPANY_OPTIONS} />
          </Form.Item>
          <Form.Item name="workTypeId" label="工种" rules={[{ required: true, message: '请选择工种' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="请选择工种"
              options={workTypeGroupedOptions}
            />
          </Form.Item>
          <Form.Item name="workTypeLevel" label="等级" rules={[{ required: true, message: '请选择工种等级' }]}>
            <Select placeholder="请选择等级" options={WORK_LEVELS} />
          </Form.Item>
          <Form.Item name="num" label="人数" rules={[{ required: true, message: '请输入正确的人数' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入人数" />
          </Form.Item>
          <Form.Item name="traindemandId" hidden>
            <InputNumber />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
