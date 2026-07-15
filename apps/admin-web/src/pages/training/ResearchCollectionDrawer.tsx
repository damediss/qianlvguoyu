import { useEffect, useMemo, useState } from 'react';
import QRCode from 'qrcode';
import {
  Alert,
  Button,
  Descriptions,
  Divider,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  CloudDownloadOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ProjectOutlined,
  QrcodeOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { ResearchCollectionPersonForm } from './ResearchCollectionPersonForm';
import { normalizePersonFormValues } from './researchCollectionFieldOptions';
import type {
  TrainResearchOrg,
  TrainResearchPerson,
  TrainResearchPersonCreateReq,
  CertVerifyBatchResultVO,
} from '../../types/training';

const { Text, Title } = Typography;

const APP_RESEARCH_COLLECT_H5 = (import.meta.env.VITE_H5_BASE_URL || 'https://h5.gjjd168.com').replace(/\/$/, '');

/** 证书查证状态 Tag 渲染：null未查证 0查证中 1已查证 2无结果 3失败 */
function renderCertVerifyTag(certVerified?: number) {
  const m =
    certVerified === 1
      ? { color: 'success', text: '已查证' }
      : certVerified === 2
        ? { color: 'warning', text: '无结果' }
        : certVerified === 3
          ? { color: 'error', text: '失败' }
          : certVerified === 0
            ? { color: 'processing', text: '查证中' }
            : { color: 'default', text: '未查证' };
  return <Tag color={m.color}>{m.text}</Tag>;
}

function buildCollectPageUrl(token: string, fromApi?: string | null): string {
  if (fromApi) return fromApi;
  return `${APP_RESEARCH_COLLECT_H5}/#/pages/research/collect?token=${encodeURIComponent(token)}`;
}

export interface ResearchCollectionDrawerProps {
  open: boolean;
  researchOrg: TrainResearchOrg | null;
  onClose: () => void;
}

export function ResearchCollectionDrawer({ open, researchOrg, onClose }: ResearchCollectionDrawerProps) {
  const queryClient = useQueryClient();
  const [peoplePage, setPeoplePage] = useState(1);
  const [peopleKeyword, setPeopleKeyword] = useState('');
  const [personModalOpen, setPersonModalOpen] = useState(false);
  const [editingPerson, setEditingPerson] = useState<TrainResearchPerson | null>(null);
  const [personForm] = Form.useForm<TrainResearchPersonCreateReq>();
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [selectedPeopleIds, setSelectedPeopleIds] = useState<number[]>([]);
  const [verifyResultModal, setVerifyResultModal] = useState<CertVerifyBatchResultVO | null>(null);

  const researchOrgId = researchOrg?.id;

  const { data: collection, isLoading: collectionLoading, refetch: refetchCollection } = useQuery({
    queryKey: ['research-collection', researchOrgId],
    queryFn: () => trainApi.getResearchCollection(researchOrgId!),
    enabled: open && !!researchOrgId,
  });

  const peopleQuery = useMemo(
    () => ({ page: peoplePage, pageSize: 10, keyword: peopleKeyword || undefined }),
    [peoplePage, peopleKeyword],
  );

  const { data: peopleData, isLoading: peopleLoading, refetch: refetchPeople } = useQuery({
    queryKey: ['research-people', researchOrgId, peopleQuery],
    queryFn: () => trainApi.pageResearchPeople(researchOrgId!, peopleQuery),
    enabled: open && !!researchOrgId && collection?.collectionStatus === 'active',
  });

  useEffect(() => {
    if (!open) {
      setQrDataUrl('');
      return;
    }
    const token = collection?.qrcodeToken;
    if (!token || collection?.collectionStatus !== 'active') {
      setQrDataUrl('');
      return;
    }
    const pageUrl = buildCollectPageUrl(token, collection.collectionPageUrl);
    QRCode.toDataURL(pageUrl, { width: 180, margin: 1, errorCorrectionLevel: 'M' })
      .then(setQrDataUrl)
      .catch(() => setQrDataUrl(''));
  }, [open, collection?.qrcodeToken, collection?.collectionPageUrl, collection?.collectionStatus]);

  useEffect(() => {
    if (!open) {
      setPeoplePage(1);
      setPeopleKeyword('');
      setPersonModalOpen(false);
      setEditingPerson(null);
      setSelectedPeopleIds([]);
      setVerifyResultModal(null);
      personForm.resetFields();
    }
  }, [open, personForm]);

  const startMu = useMutation({
    mutationFn: () => trainApi.startResearchCollection(researchOrgId!),
    onSuccess: () => {
      message.success('已开启人员采集');
      refetchCollection();
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: (err: Error) => message.error(err.message || '开启采集失败'),
  });

  const savePersonMu = useMutation({
    mutationFn: (values: TrainResearchPersonCreateReq) => {
      if (editingPerson) {
        return trainApi.updateResearchPerson(researchOrgId!, editingPerson.id, values);
      }
      return trainApi.createResearchPerson(researchOrgId!, values);
    },
    onSuccess: () => {
      message.success(editingPerson ? '更新成功' : '添加成功');
      setPersonModalOpen(false);
      setEditingPerson(null);
      personForm.resetFields();
      refetchPeople();
      refetchCollection();
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: (err: Error) => message.error(err.message || '保存失败'),
  });

  const deletePersonMu = useMutation({
    mutationFn: (peopleId: number) => trainApi.deleteResearchPerson(researchOrgId!, peopleId),
    onSuccess: () => {
      message.success('删除成功');
      refetchPeople();
      refetchCollection();
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: () => message.error('删除失败'),
  });

  const exportMu = useMutation({
    mutationFn: () => trainApi.downloadResearchCollectionTemplate(
      researchOrgId!,
      researchOrg?.title ?? '调研企业',
    ),
    onSuccess: () => message.success('导出成功'),
    onError: (err: Error) => message.error(err.message || '导出失败'),
  });

  const createPlanMu = useMutation({
    mutationFn: () => trainApi.createThreeYearPlanFromResearch(researchOrgId!, {
      title: `${researchOrg?.title ?? ''}三年规划`,
    }),
    onSuccess: (plan) => {
      message.success(`三年规划已创建：${plan.title}，请前往「方案规划」查看分析进度`);
      queryClient.invalidateQueries({ queryKey: ['three-year-plans'] });
    },
    onError: (err: Error) => message.error(err.message || '创建三年规划失败'),
  });

  const verifyCertMu = useMutation({
    mutationFn: (peopleIds: number[]) =>
      trainApi.verifyResearchPeopleCertificate(researchOrgId!, peopleIds),
    onSuccess: (result) => {
      setVerifyResultModal(result);
      setSelectedPeopleIds([]);
      refetchPeople();
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: (err: Error) => message.error(err.message || '查证失败'),
  });

  const openCreatePerson = () => {
    setEditingPerson(null);
    personForm.setFieldsValue({
      companyName: researchOrg?.title,
      certificates: [{ level: undefined, certNo: undefined, issueDate: undefined }],
    });
    setPersonModalOpen(true);
  };

  const openEditPerson = (person: TrainResearchPerson) => {
    setEditingPerson(person);
    personForm.setFieldsValue({
      ...person,
      certificates: person.certificates?.length
        ? person.certificates
        : [{ level: undefined, certNo: undefined, issueDate: undefined }],
    });
    setPersonModalOpen(true);
  };

  const peopleColumns: ColumnsType<TrainResearchPerson> = [
    { title: '姓名', dataIndex: 'name', width: 90 },
    { title: '身份证号', dataIndex: 'idCardNumber', width: 170, ellipsis: true },
    { title: '岗位', dataIndex: 'job', width: 120, ellipsis: true },
    { title: '原工种', dataIndex: 'workTypeName', width: 120, ellipsis: true },
    { title: '学历', dataIndex: 'education', width: 90 },
    {
      title: '查证状态',
      dataIndex: 'certVerified',
      width: 100,
      align: 'center',
      render: (v: number | undefined) => renderCertVerifyTag(v),
    },
    {
      title: '操作',
      key: 'act',
      width: 180,
      render: (_, row) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditPerson(row)}>
            编辑
          </Button>
          <Tooltip title="单条查证">
            <Button
              type="link"
              size="small"
              icon={<SafetyCertificateOutlined />}
              loading={verifyCertMu.isPending && Array.isArray(verifyCertMu.variables) && verifyCertMu.variables.length === 1 && verifyCertMu.variables[0] === row.id}
              onClick={() => verifyCertMu.mutate([row.id])}
            >
              查证
            </Button>
          </Tooltip>
          <Popconfirm title="确定删除该人员吗？" onConfirm={() => deletePersonMu.mutate(row.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const collectionActive = collection?.collectionStatus === 'active';
  const personnelCount = collection?.personnelCount ?? researchOrg?.personnelCount ?? 0;
  const fieldOptions = collection?.fieldOptions;

  return (
    <>
      <Drawer
        title={`人员采集 - ${researchOrg?.title ?? ''}`}
        width={920}
        open={open}
        onClose={onClose}
        destroyOnClose
      >
        {!researchOrg?.targetOrgId && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="请先在编辑调研企业时关联系统企业，才能开启采集和创建三年规划"
          />
        )}

        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="调研企业">{researchOrg?.title}</Descriptions.Item>
          <Descriptions.Item label="系统企业">{researchOrg?.targetOrgName || collection?.targetOrgName || '-'}</Descriptions.Item>
          <Descriptions.Item label="采集状态">
            {collectionActive ? '采集中' : '未开启'}
          </Descriptions.Item>
          <Descriptions.Item label="已采集人数">{personnelCount}</Descriptions.Item>
        </Descriptions>

        <Divider />

        <div style={{ display: 'flex', gap: 24, marginBottom: 24, flexWrap: 'wrap' }}>
          <div
            style={{
              width: 200,
              height: 200,
              border: '1px dashed #d9d9d9',
              borderRadius: 8,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              background: '#fafafa',
              flexShrink: 0,
              overflow: 'hidden',
            }}
          >
            {collectionActive && qrDataUrl ? (
              <img src={qrDataUrl} alt="人员采集二维码" style={{ width: 180, height: 180 }} />
            ) : (
              <>
                <QrcodeOutlined style={{ fontSize: 48, color: '#bfbfbf', marginBottom: 12 }} />
                <Text type="secondary" style={{ textAlign: 'center', padding: '0 12px' }}>
                  {collectionActive ? '二维码生成中' : '开启采集后显示二维码'}
                </Text>
              </>
            )}
          </div>

          <div style={{ flex: 1, minWidth: 260 }}>
            <Title level={5} style={{ marginTop: 0 }}>采集操作</Title>
            <Space direction="vertical" style={{ width: '100%' }}>
              {!collectionActive ? (
                <Button
                  type="primary"
                  loading={startMu.isPending || collectionLoading}
                  disabled={!researchOrg?.targetOrgId}
                  onClick={() => startMu.mutate()}
                >
                  开启人员采集
                </Button>
              ) : (
                <>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreatePerson}>
                    手动添加人员
                  </Button>
                  <Button
                    icon={<CloudDownloadOutlined />}
                    loading={exportMu.isPending}
                    disabled={personnelCount === 0}
                    onClick={() => exportMu.mutate()}
                  >
                    导出精简版采集模板
                  </Button>
                  <Button
                    icon={<ProjectOutlined />}
                    loading={createPlanMu.isPending}
                    disabled={personnelCount === 0 || !researchOrg?.targetOrgId}
                    onClick={() => {
                      Modal.confirm({
                        title: '创建三年规划',
                        content: `将使用当前 ${personnelCount} 名人员数据自动创建三年规划并启动分析，是否继续？`,
                        onOk: () => createPlanMu.mutateAsync(),
                      });
                    }}
                  >
                    创建三年规划
                  </Button>
                </>
              )}
              {collection?.qrcodeToken && collectionActive && (
                <Text
                  type="secondary"
                  copyable={{
                    text: buildCollectPageUrl(collection.qrcodeToken, collection.collectionPageUrl),
                  }}
                  style={{ wordBreak: 'break-all' }}
                >
                  H5 采集链接
                </Text>
              )}
            </Space>
          </div>
        </div>

        {collectionActive && (
          <>
            <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
              <Input.Search
                placeholder="搜索姓名/身份证/手机"
                allowClear
                style={{ maxWidth: 320 }}
                onSearch={(v) => {
                  setPeoplePage(1);
                  setPeopleKeyword(v.trim());
                }}
              />
              <Space>
                <Button
                  type="primary"
                  icon={<SafetyCertificateOutlined />}
                  disabled={selectedPeopleIds.length === 0}
                  loading={verifyCertMu.isPending && Array.isArray(verifyCertMu.variables) && verifyCertMu.variables.length > 1}
                  onClick={() => verifyCertMu.mutate(selectedPeopleIds)}
                >
                  查证{selectedPeopleIds.length > 0 ? ` (${selectedPeopleIds.length})` : ''}
                </Button>
              </Space>
            </div>
            <Table<TrainResearchPerson>
              rowKey="id"
              size="small"
              rowSelection={{
                selectedRowKeys: selectedPeopleIds,
                onChange: (keys) => setSelectedPeopleIds(keys as number[]),
              }}
              columns={peopleColumns}
              dataSource={peopleData?.items ?? []}
              loading={peopleLoading}
              pagination={{
                current: peoplePage,
                pageSize: 10,
                total: peopleData?.total ?? 0,
                onChange: setPeoplePage,
                showTotal: (t) => `共 ${t} 人`,
              }}
              scroll={{ x: 900 }}
            />
          </>
        )}
      </Drawer>

      <Modal
        title="查证完成"
        open={!!verifyResultModal}
        onCancel={() => setVerifyResultModal(null)}
        footer={[
          <Button key="close" onClick={() => setVerifyResultModal(null)}>
            关闭
          </Button>,
        ]}
        width={720}
        destroyOnClose
      >
        {verifyResultModal && (
          <>
            <Descriptions column={4} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="总数">{verifyResultModal.total}</Descriptions.Item>
              <Descriptions.Item label="成功">
                <span style={{ color: '#52c41a', fontWeight: 600 }}>{verifyResultModal.verified}</span>
              </Descriptions.Item>
              <Descriptions.Item label="无结果">
                <span style={{ color: '#faad14', fontWeight: 600 }}>{verifyResultModal.notFound}</span>
              </Descriptions.Item>
              <Descriptions.Item label="失败">
                <span style={{ color: '#ff4d4f', fontWeight: 600 }}>{verifyResultModal.failed}</span>
              </Descriptions.Item>
            </Descriptions>
            {verifyResultModal.details.length > 0 && (
              <Table<CertVerifyBatchResultVO['details'][number]>
                rowKey="peopleId"
                size="small"
                pagination={false}
                scroll={{ y: 320 }}
                dataSource={verifyResultModal.details}
                columns={[
                  { title: '姓名', dataIndex: 'name', width: 90 },
                  { title: '身份证号', dataIndex: 'idCardNumber', width: 170, ellipsis: true },
                  {
                    title: '查证结果',
                    dataIndex: 'found',
                    width: 90,
                    render: (v: boolean) =>
                      v ? <Tag color="success">已查到</Tag> : <Tag color="warning">无结果</Tag>,
                  },
                  { title: '证书等级', dataIndex: 'certLevel', width: 90 },
                  { title: '证书编号', dataIndex: 'certNo', ellipsis: true },
                  { title: '发证日期', dataIndex: 'issueDate', width: 110 },
                  {
                    title: '错误',
                    dataIndex: 'errorMessage',
                    ellipsis: true,
                    render: (v?: string) => v || '-',
                  },
                ]}
              />
            )}
          </>
        )}
      </Modal>

      <Modal
        title={editingPerson ? '编辑人员' : '添加人员'}
        open={personModalOpen}
        onCancel={() => {
          setPersonModalOpen(false);
          setEditingPerson(null);
          personForm.resetFields();
        }}
        onOk={() => personForm.submit()}
        confirmLoading={savePersonMu.isPending}
        width={720}
        destroyOnClose
      >
        <Form
          form={personForm}
          layout="vertical"
          onFinish={(values) =>
            savePersonMu.mutate(
              normalizePersonFormValues(values as unknown as Record<string, unknown>) as unknown as TrainResearchPersonCreateReq,
            )
          }
        >
          <ResearchCollectionPersonForm
            fieldOptions={fieldOptions}
            companyName={researchOrg?.title}
          />
        </Form>
      </Modal>
    </>
  );
}
