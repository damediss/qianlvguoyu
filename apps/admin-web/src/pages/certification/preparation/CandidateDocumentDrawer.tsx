import {
  CheckOutlined,
  DeleteOutlined,
  EyeOutlined,
  RollbackOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { Button, Descriptions, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, Upload, message } from 'antd';
import type { UploadProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { uploadFile } from '../../../api/knowledge';
import {
  pagePreparationCandidateDocuments,
  deletePreparationCandidateDocument,
  reviewPreparationCandidateDocument,
  uploadPreparationCandidateDocument,
} from '../../../api/certificationPreparation';
import { FilePreviewModal, type FilePreviewTarget } from '../../../components/FilePreviewModal';
import { PermissionGate } from '../../../directives/permission';
import type { FileVO } from '../../../types/knowledge';
import type {
  PreparationCandidate,
  PreparationCandidateDocument,
  PreparationCandidateDocumentPayload,
  PreparationDocumentReviewPayload,
} from '../../../types/certificationPreparation';
import { boolTag, candidateDocumentRatioTag, dash, dateTimeText, statusTag } from './shared';

interface CandidateDocumentDrawerProps {
  taskId: string;
  candidate: PreparationCandidate | null;
  open: boolean;
  onClose: () => void;
  onChanged?: () => void;
}

export function CandidateDocumentDrawer({ taskId, candidate, open, onClose, onChanged }: CandidateDocumentDrawerProps) {
  const queryClient = useQueryClient();
  const [detailStatus, setDetailStatus] = useState<string>();
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [uploadingDocument, setUploadingDocument] = useState<PreparationCandidateDocument | null>(null);
  const [reviewingDocument, setReviewingDocument] = useState<PreparationCandidateDocument | null>(null);
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);
  const [uploadingFile, setUploadingFile] = useState(false);
  const [uploadForm] = Form.useForm<PreparationCandidateDocumentPayload>();
  const [reviewForm] = Form.useForm<PreparationDocumentReviewPayload>();

  const documentsQuery = useQuery({
    queryKey: ['certification-preparation-documents', taskId, candidate?.id, detailStatus],
    queryFn: () =>
      pagePreparationCandidateDocuments(taskId, {
        candidateId: candidate!.id,
        status: detailStatus,
        page: 1,
        pageSize: 50,
      }),
    enabled: open && Boolean(candidate?.id),
  });

  useEffect(() => {
    if (!open) {
      setDetailStatus(undefined);
      setUploadModalOpen(false);
      setUploadingDocument(null);
      setReviewingDocument(null);
      setPreviewTarget(null);
    }
  }, [open]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-documents', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    onChanged?.();
  };

  const saveMutation = useMutation({
    mutationFn: (payload: PreparationCandidateDocumentPayload) => uploadPreparationCandidateDocument(taskId, payload),
    onSuccess: () => {
      message.success('资料已保存');
      setUploadModalOpen(false);
      setUploadingDocument(null);
      uploadForm.resetFields();
      refresh();
    },
  });

  const reviewMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PreparationDocumentReviewPayload }) =>
      reviewPreparationCandidateDocument(taskId, id, payload),
    onSuccess: () => {
      message.success('审核结果已保存');
      setReviewingDocument(null);
      reviewForm.resetFields();
      refresh();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (documentId: string) => deletePreparationCandidateDocument(taskId, documentId),
    onSuccess: () => {
      message.success('资料已删除，可重新上传');
      refresh();
    },
  });

  const uploadProps: UploadProps = {
    maxCount: 1,
    beforeUpload: async (file) => {
      const docCode = uploadForm.getFieldValue('docCode') || uploadingDocument?.docCode;
      const validationError = validateUploadFileType(docCode, file);
      if (validationError) {
        message.warning(validationError);
        return Upload.LIST_IGNORE;
      }
      setUploadingFile(true);
      try {
        const saved: FileVO = await uploadFile(file, 'private');
        const fileId = typeof saved.id === 'string' ? saved.id : String(saved.id);
        uploadForm.setFieldsValue({
          fileId,
          fileName: saved.originalName,
          fileType: saved.contentType,
        });
        message.success('文件已上传到统一文件服务');
      } finally {
        setUploadingFile(false);
      }
      return false;
    },
  };

  const documentColumns: ColumnsType<PreparationCandidateDocument> = [
    { title: '资料编码', dataIndex: 'docCode', width: 100 },
    { title: '资料名称', dataIndex: 'docName', minWidth: 200 },
    { title: '必传', dataIndex: 'required', width: 72, render: boolTag },
    { title: '文件', dataIndex: 'fileName', minWidth: 140, render: dash },
    {
      title: '预览',
      width: 80,
      align: 'center',
      render: (_, record) =>
        record.fileId && record.status !== 'missing' ? (
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() =>
              setPreviewTarget({
                taskId,
                documentId: record.id,
                fileId: record.fileId,
                fileName: record.fileName,
                fileType: record.fileType,
              })
            }
          >
            预览
          </Button>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    { title: '上传时间', dataIndex: 'uploadedAt', width: 150, render: dateTimeText },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      render: (_, record) => {
        const reviewed = record.status === 'approved';
        if (record.status === 'missing') {
          return (
            <PermissionGate code="certification.preparation.document.upload">
              <Button type="link" size="small" icon={<UploadOutlined />} onClick={() => openUploadModal(record)}>
                待上传
              </Button>
            </PermissionGate>
          );
        }
        if (record.status === 'rejected') {
          return (
            <PermissionGate code="certification.preparation.document.upload">
              <Popconfirm
                title="确认删除该驳回资料？"
                okText="删除"
                cancelText="取消"
                onConfirm={() => deleteMutation.mutate(record.id)}
              >
                <Button type="link" size="small" danger icon={<DeleteOutlined />} loading={deleteMutation.isPending}>
                  删除
                </Button>
              </Popconfirm>
            </PermissionGate>
          );
        }
        return (
          <PermissionGate code="certification.preparation.document.review">
            <Space size={4}>
              <Button
                type="link"
                size="small"
                icon={<CheckOutlined />}
                disabled={reviewed}
                onClick={() => openReview(record, 'approved')}
              >
                通过
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<RollbackOutlined />}
                disabled={reviewed}
                onClick={() => openReview(record, 'rejected')}
              >
                驳回
              </Button>
            </Space>
          </PermissionGate>
        );
      },
    },
  ];

  function openUploadModal(record: PreparationCandidateDocument) {
    if (!candidate) {
      return;
    }
    uploadForm.resetFields();
    setUploadingDocument(record);
    uploadForm.setFieldsValue({
      candidateId: candidate.id,
      docCode: record.docCode,
      docName: record.docName,
      required: record.required,
    });
    setUploadModalOpen(true);
  }

  function openReview(record: PreparationCandidateDocument, status: string) {
    setReviewingDocument(record);
    reviewForm.setFieldsValue({ status, reviewComment: status === 'approved' ? '资料审核通过' : undefined });
  }

  function submitUpload() {
    uploadForm.validateFields().then((values) => saveMutation.mutate(values));
  }

  function submitReview() {
    if (!reviewingDocument) {
      return;
    }
    reviewForm.validateFields().then((values) => {
      reviewMutation.mutate({ id: reviewingDocument.id, payload: values });
    });
  }

  return (
    <>
      <Modal
        title={candidate ? `${candidate.candidateName} — 学员资料` : '学员资料'}
        open={open}
        onCancel={onClose}
        width={1080}
        footer={null}
        destroyOnClose
      >
        {candidate ? (
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <Descriptions bordered size="small" column={3}>
              <Descriptions.Item label="姓名">{candidate.candidateName}</Descriptions.Item>
              <Descriptions.Item label="身份证号">{candidate.maskedIdCardNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="联系电话">{candidate.maskedPhone || '-'}</Descriptions.Item>
              <Descriptions.Item label="报考等级">{candidate.levelName || '-'}</Descriptions.Item>
              <Descriptions.Item label="资料进度">{candidateDocumentRatioTag(candidate)}</Descriptions.Item>
            </Descriptions>
            <Space style={{ justifyContent: 'flex-start', width: '100%' }}>
              <Select
                allowClear
                placeholder="按资料状态筛选"
                style={{ width: 180 }}
                value={detailStatus}
                onChange={setDetailStatus}
                options={[
                  { label: '未导入', value: 'missing' },
                  { label: '待审核', value: 'pending_review' },
                  { label: '已上传', value: 'uploaded' },
                  { label: '已通过', value: 'approved' },
                  { label: '已驳回', value: 'rejected' },
                ]}
              />
            </Space>
            <Table
              rowKey="id"
              size="small"
              columns={documentColumns}
              dataSource={documentsQuery.data?.items || []}
              loading={documentsQuery.isFetching}
              scroll={{ x: 1000 }}
              pagination={false}
            />
          </Space>
        ) : null}
      </Modal>

      <Modal
        title="上传学员资料"
        open={uploadModalOpen}
        onOk={submitUpload}
        onCancel={() => {
          setUploadModalOpen(false);
          setUploadingDocument(null);
        }}
        confirmLoading={saveMutation.isPending || uploadingFile}
        width={720}
        destroyOnClose
      >
        <Form form={uploadForm} layout="vertical">
          <Form.Item name="candidateId" hidden rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="docCode" label="资料类型" rules={[{ required: true, message: '请选择资料类型' }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item name="docName" label="资料名称" rules={[{ required: true, message: '请输入资料名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="required" label="是否必传">
            <Select options={[{ label: '是', value: true }, { label: '否', value: false }]} />
          </Form.Item>
          <Form.Item label="文件">
            <Upload {...uploadProps} accept={acceptByDocCode(uploadingDocument?.docCode)}>
              <Button icon={<UploadOutlined />} loading={uploadingFile}>
                选择并上传文件
              </Button>
            </Upload>
          </Form.Item>
          <Form.Item name="fileId" label="文件ID" rules={[{ required: true, message: '请先上传文件' }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item name="fileName" label="文件名" rules={[{ required: true, message: '请先上传文件' }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item name="fileType" label="文件类型">
            <Input disabled />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <FilePreviewModal open={Boolean(previewTarget)} target={previewTarget} onClose={() => setPreviewTarget(null)} />

      <Modal
        title="资料审核"
        open={Boolean(reviewingDocument)}
        onOk={submitReview}
        onCancel={() => setReviewingDocument(null)}
        confirmLoading={reviewMutation.isPending}
        destroyOnClose
      >
        <Form form={reviewForm} layout="vertical">
          <Form.Item name="status" label="审核结果" rules={[{ required: true, message: '请选择审核结果' }]}>
            <Select options={[{ label: '通过', value: 'approved' }, { label: '驳回', value: 'rejected' }]} />
          </Form.Item>
          <Form.Item name="reviewComment" label="审核意见">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function acceptByDocCode(docCode?: string) {
  if (docCode === 'DOC001') {
    return '.xlsx,.pdf,.docx';
  }
  if (docCode === 'DOC002') {
    return 'image/*';
  }
  return undefined;
}

function validateUploadFileType(docCode: string | undefined, file: File) {
  const extension = file.name.includes('.') ? file.name.substring(file.name.lastIndexOf('.')).toLowerCase() : '';
  if (docCode === 'DOC001' && !['.xlsx', '.pdf', '.docx'].includes(extension)) {
    return '报名信息表仅支持 xlsx、pdf、docx 格式';
  }
  if (docCode === 'DOC002') {
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'];
    if (!file.type.startsWith('image/') && !imageExtensions.includes(extension)) {
      return '两寸证件照仅支持图片格式';
    }
  }
  return undefined;
}
