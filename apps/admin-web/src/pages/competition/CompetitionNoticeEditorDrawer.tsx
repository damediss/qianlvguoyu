import {
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  FilePdfOutlined,
  InboxOutlined,
} from '@ant-design/icons';
import { Alert, Button, Descriptions, Drawer, Form, Input, InputNumber, Modal, Radio, Space, Spin, Tag, Upload, message } from 'antd';
import type { UploadProps } from 'antd';
import { MdEditor, MdPreview } from 'md-editor-rt';
import 'md-editor-rt/lib/style.css';
import MarkdownIt from 'markdown-it';
import type Token from 'markdown-it/lib/token.mjs';
import sanitizeHtml from 'sanitize-html';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  createCompetitionNotice,
  deleteCompetitionNoticeAsset,
  getCompetitionNotice,
  getCompetitionNoticeAsset,
  updateCompetitionNotice,
  uploadCompetitionNoticeAsset,
} from '../../api/competition';
import type {
  CompetitionId,
  CompetitionNoticeAsset,
  CompetitionNoticeContentType,
  CompetitionNoticeCreateRequest,
} from '../../types/competition';

type NoticeMode = 'create' | 'edit' | 'view';

interface NoticeFormValues {
  title: string;
  content: string;
  contentType: CompetitionNoticeContentType;
  version: number;
}

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const IMAGE_MAX_SIZE = 10 * 1024 * 1024;
const PDF_MAX_SIZE = 50 * 1024 * 1024;
const markdownParser = new MarkdownIt({ html: true });
markdownParser.validateLink = () => true;

const contentTypeMeta: Record<CompetitionNoticeContentType, { label: string; color: string }> = {
  markdown: { label: '图文公告', color: 'blue' },
  pdf: { label: 'PDF 赛事文件', color: 'purple' },
};

function sanitizeNoticeHtml(html: string) {
  return sanitizeHtml(html, {
    allowedTags: [
      'p', 'br', 'strong', 'em', 's', 'blockquote', 'ul', 'ol', 'li', 'h1', 'h2', 'h3',
      'h4', 'h5', 'h6', 'pre', 'code', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'a', 'img',
    ],
    allowedAttributes: {
      a: ['href', 'title', 'target', 'rel'],
      img: ['src', 'alt', 'title'],
      code: ['class'],
    },
    allowedSchemes: ['http', 'https', 'mailto', 'blob'],
    allowProtocolRelative: false,
    transformTags: {
      img: (tagName, attribs) => ({
        tagName,
        attribs: {
          ...attribs,
          src: attribs.src?.startsWith('blob:') ? attribs.src : '',
        },
      }),
      a: (tagName, attribs) => ({
        tagName,
        attribs: {
          ...attribs,
          href: /^(https?:|mailto:)/i.test(attribs.href || '') ? attribs.href : '',
          target: '_blank',
          rel: 'noopener noreferrer',
        },
      }),
    },
  });
}

function fileSize(size: number) {
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  return `${Math.max(1, Math.ceil(size / 1024))} KB`;
}

function isPdf(file: File) {
  return file.type === 'application/pdf' && file.name.toLowerCase().endsWith('.pdf');
}

function validateMarkdownSource(value?: string) {
  if (!value) return Promise.resolve();
  const collectImageSources = (tokens: Token[]): string[] => tokens.flatMap((token) => {
    const ownSource = token.type === 'image' ? token.attrGet('src') : null;
    return [...(ownSource ? [ownSource] : []), ...(token.children ? collectImageSources(token.children) : [])];
  });
  const containsRawHtml = (tokens: Token[]): boolean => tokens.some((token) =>
    token.type === 'html_inline'
    || token.type === 'html_block'
    || Boolean(token.children && containsRawHtml(token.children)),
  );
  const tokens = markdownParser.parse(value, {});
  if (containsRawHtml(tokens)) {
    return Promise.reject(new Error('Markdown 正文不允许使用原始 HTML'));
  }
  const imageSources = collectImageSources(tokens);
  if (imageSources.some((source) => !/^notice-asset:\/\/\d+$/.test(source))) {
    return Promise.reject(new Error('图片必须通过编辑器上传，不允许使用外链、data: 或 javascript: 地址'));
  }
  const containsUnsafeLink = (items: Token[]): boolean => items.some((token) => {
    const href = token.type === 'link_open' ? token.attrGet('href') : null;
    const normalizedHref = href?.replace(/[\u0000-\u0020\u007f]+/g, '').toLowerCase();
    return normalizedHref?.startsWith('javascript:')
      || normalizedHref?.startsWith('data:')
      || Boolean(token.children && containsUnsafeLink(token.children));
  });
  if (containsUnsafeLink(tokens)) {
    return Promise.reject(new Error('链接不允许使用 javascript: 或 data: 地址'));
  }
  return Promise.resolve();
}

export function CompetitionNoticeEditorDrawer({
  eventId,
  mode,
  noticeId,
  onClose,
  onSaved,
}: {
  eventId: CompetitionId;
  mode?: NoticeMode;
  noticeId?: CompetitionId;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form] = Form.useForm<NoticeFormValues>();
  const [draftId, setDraftId] = useState<CompetitionId | undefined>(noticeId);
  const [assets, setAssets] = useState<CompetitionNoticeAsset[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [previewing, setPreviewing] = useState(false);
  const [contentType, setContentType] = useState<CompetitionNoticeContentType>('markdown');
  const [readOnlyMarkdown, setReadOnlyMarkdown] = useState('');
  const objectUrls = useRef(new Map<string, string>());
  const readOnly = mode === 'view';
  const open = Boolean(mode);
  const sessionGeneration = useRef(0);
  const sessionKeyRef = useRef('');
  const sessionKey = `${open}:${mode || ''}:${noticeId == null ? '' : String(noticeId)}`;
  if (sessionKeyRef.current !== sessionKey) {
    sessionKeyRef.current = sessionKey;
    sessionGeneration.current += 1;
  }

  const revokeObjectUrls = useCallback(() => {
    objectUrls.current.forEach((url) => URL.revokeObjectURL(url));
    objectUrls.current.clear();
  }, []);

  useEffect(() => {
    let cancelled = false;
    const generation = sessionGeneration.current;
    const isStale = () => cancelled || sessionGeneration.current !== generation;
    if (!open) {
      revokeObjectUrls();
      setDraftId(undefined);
      setAssets([]);
      setReadOnlyMarkdown('');
      setLoading(false);
      form.resetFields();
      return () => {
        cancelled = true;
      };
    }
    revokeObjectUrls();
    if (!noticeId) {
      setDraftId(undefined);
      setAssets([]);
      setReadOnlyMarkdown('');
      setContentType('markdown');
      form.resetFields();
      form.setFieldsValue({ contentType: 'markdown', content: '', version: 1 });
      return () => {
        cancelled = true;
      };
    }
    setLoading(true);
    void (async () => {
      const notice = await getCompetitionNotice(noticeId);
      if (isStale()) return;
        const type = notice.contentType || 'markdown';
        setDraftId(notice.id);
        setAssets(notice.assets || []);
        setContentType(type);
        form.setFieldsValue({
          title: notice.title,
          content: notice.content || '',
          contentType: type,
          version: notice.version,
        });
        if (mode === 'view' && type === 'markdown') {
          const replacements = await Promise.all((notice.assets || [])
            .filter((asset) => asset.assetType === 'inline_image')
            .map(async (asset) => {
              const blob = await getCompetitionNoticeAsset(notice.id, asset.assetId);
              if (isStale()) return null;
              const url = URL.createObjectURL(blob);
              objectUrls.current.set(String(asset.assetId), url);
              return [`notice-asset://${asset.assetId}`, url] as const;
            }));
          if (!isStale()) {
            setReadOnlyMarkdown(replacements.filter((item) => item !== null).reduce(
              (content, [source, url]) => content.replaceAll(source, url),
              notice.content || '',
            ));
          }
        } else {
          setReadOnlyMarkdown(notice.content || '');
        }
    })().finally(() => {
      if (!isStale()) setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [form, mode, noticeId, open, revokeObjectUrls]);

  useEffect(() => () => revokeObjectUrls(), [revokeObjectUrls]);

  const payloadFromForm = (): CompetitionNoticeCreateRequest => {
    const values = form.getFieldsValue();
    return {
      title: values.title?.trim() || '',
      content: values.content?.trim() || '',
      contentType: values.contentType || contentType,
      version: values.version || 1,
    };
  };

  const ensureDraft = async () => {
    if (draftId) return draftId;
    const title = form.getFieldValue('title')?.trim();
    if (!title) {
      await form.validateFields(['title']);
      throw new Error('请先填写公告标题');
    }
    const created = await createCompetitionNotice(eventId, payloadFromForm());
    setDraftId(created.id);
    setAssets(created.assets || []);
    form.setFieldValue('version', created.version);
    message.success('已自动保存草稿');
    onSaved();
    return created.id;
  };

  const uploadImages = async (files: File[], callback: (urls: string[]) => void) => {
    const invalid = files.find((file) => !IMAGE_TYPES.includes(file.type) || file.size > IMAGE_MAX_SIZE);
    if (invalid) {
      message.error('仅支持不超过 10MB 的 JPG、PNG、WebP 图片');
      return;
    }
    setUploading(true);
    try {
      const id = await ensureDraft();
      const uploaded = await Promise.all(
        files.map((file) => uploadCompetitionNoticeAsset(id, file, 'inline_image')),
      );
      setAssets((current) => [...current, ...uploaded]);
      callback(uploaded.map((asset) => `notice-asset://${asset.assetId}`));
    } finally {
      setUploading(false);
    }
  };

  const resolveAssetUrl = useCallback(async (source: string) => {
    if (!source.startsWith('notice-asset://') || !draftId) return '';
    const generation = sessionGeneration.current;
    const assetId = source.slice('notice-asset://'.length);
    const cached = objectUrls.current.get(assetId);
    if (cached) return cached;
    const blob = await getCompetitionNoticeAsset(draftId, assetId);
    if (sessionGeneration.current !== generation) return '';
    const url = URL.createObjectURL(blob);
    objectUrls.current.set(assetId, url);
    return url;
  }, [draftId]);

  const removeAsset = async (asset: CompetitionNoticeAsset) => {
    if (!draftId) return;
    await deleteCompetitionNoticeAsset(draftId, asset.assetId);
    const key = String(asset.assetId);
    const url = objectUrls.current.get(key);
    if (url) URL.revokeObjectURL(url);
    objectUrls.current.delete(key);
    setAssets((current) => current.filter((item) => String(item.assetId) !== key));
    if (asset.assetType === 'inline_image') {
      const content = form.getFieldValue('content') || '';
      form.setFieldValue('content', content.replaceAll(`notice-asset://${asset.assetId}`, ''));
    }
    message.success('文件已删除');
  };

  const changeContentType = async (next: CompetitionNoticeContentType) => {
    if (next === contentType) return;
    const incompatible = assets.filter((asset) =>
      next === 'markdown' ? asset.assetType === 'pdf' : asset.assetType === 'inline_image',
    );
    const currentContent = form.getFieldValue('content') || '';
    const confirmed = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '切换公告类型？',
        content: incompatible.length > 0
          ? `切换后将清空当前正文，并由服务端事务删除 ${incompatible.length} 个不兼容文件，且无法恢复。`
          : currentContent
            ? '切换后将清空当前正文，且无法恢复。'
            : '确认切换公告发布类型？',
        okText: '切换并清理',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      });
    });
    if (!confirmed) {
      form.setFieldValue('contentType', contentType);
      return;
    }
    if (draftId) {
      try {
        const updated = await updateCompetitionNotice(draftId, {
          ...payloadFromForm(),
          content: '',
          contentType: next,
        });
        const retainedIds = new Set((updated.assets || []).map((asset) => String(asset.assetId)));
        assets.forEach((asset) => {
          const key = String(asset.assetId);
          if (!retainedIds.has(key)) {
            const url = objectUrls.current.get(key);
            if (url) URL.revokeObjectURL(url);
            objectUrls.current.delete(key);
          }
        });
        setAssets(updated.assets || []);
        setContentType(updated.contentType || next);
        form.setFieldsValue({
          title: updated.title,
          content: updated.content || '',
          contentType: updated.contentType || next,
          version: updated.version,
        });
        onSaved();
      } catch {
        form.setFieldsValue({ content: currentContent, contentType });
      }
      return;
    }
    form.setFieldValue('content', '');
    setContentType(next);
  };

  const uploadPdf: UploadProps['beforeUpload'] = async (file) => {
    if (!isPdf(file) || file.size > PDF_MAX_SIZE) {
      message.error('仅支持不超过 50MB 的 PDF 文件');
      return Upload.LIST_IGNORE;
    }
    setUploading(true);
    try {
      const id = await ensureDraft();
      const oldPdf = assets.find((asset) => asset.assetType === 'pdf');
      const uploaded = await uploadCompetitionNoticeAsset(id, file, 'pdf');
      setAssets((current) => [...current.filter((asset) => asset.assetType !== 'pdf'), uploaded]);
      message.success(oldPdf ? 'PDF 已替换' : 'PDF 上传成功');
    } finally {
      setUploading(false);
    }
    return false;
  };

  const openPdf = async (download: boolean) => {
    const pdf = assets.find((asset) => asset.assetType === 'pdf');
    if (!draftId || !pdf) return;
    setPreviewing(true);
    const generation = sessionGeneration.current;
    try {
      const blob = await getCompetitionNoticeAsset(draftId, pdf.assetId);
      if (sessionGeneration.current !== generation) return;
      const url = URL.createObjectURL(blob);
      const urlKey = `pdf-preview-${Date.now()}`;
      objectUrls.current.set(urlKey, url);
      const revoke = () => {
        if (objectUrls.current.get(urlKey) === url) {
          URL.revokeObjectURL(url);
          objectUrls.current.delete(urlKey);
        }
      };
      if (download) {
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = pdf.originalName || 'competition-notice.pdf';
        anchor.click();
        setTimeout(revoke, 1000);
      } else {
        window.open(url, '_blank', 'noopener,noreferrer');
        setTimeout(revoke, 60000);
      }
    } finally {
      setPreviewing(false);
    }
  };

  const submit = async () => {
    const values = await form.validateFields();
    if (values.contentType === 'pdf' && !assets.some((asset) => asset.assetType === 'pdf')) {
      message.error('请上传 PDF 赛事文件');
      return;
    }
    setSaving(true);
    try {
      const payload: CompetitionNoticeCreateRequest = {
        title: values.title.trim(),
        content: values.content?.trim() || '',
        contentType: values.contentType,
        version: values.version,
      };
      if (draftId) await updateCompetitionNotice(draftId, payload);
      else await createCompetitionNotice(eventId, payload);
      message.success(draftId ? '公告已保存' : '公告创建成功');
      onSaved();
      onClose();
    } finally {
      setSaving(false);
    }
  };

  const pdfAsset = assets.find((asset) => asset.assetType === 'pdf');
  const drawerTitle = mode === 'create' ? '创建公告' : mode === 'edit' ? '编辑公告' : '查看公告';

  return (
    <Drawer
      title={<Space>{drawerTitle}<Tag color={contentTypeMeta[contentType].color}>{contentTypeMeta[contentType].label}</Tag></Space>}
      open={open}
      width="min(1080px, 94vw)"
      destroyOnClose
      onClose={onClose}
      extra={readOnly ? <Button onClick={onClose}>关闭</Button> : <Button type="primary" loading={saving} onClick={submit}>保存草稿</Button>}
    >
      <Spin spinning={loading || uploading}>
        <Form form={form} layout="vertical" requiredMark="optional">
          <div className="competition-notice-form-grid">
            <Form.Item
              name="title"
              label="标题"
              rules={[{ required: true, whitespace: true, message: '请输入公告标题' }]}
            >
              <Input disabled={readOnly} maxLength={256} showCount placeholder="请输入公告标题" />
            </Form.Item>
            <Form.Item name="version" label="版本" rules={[{ required: true, type: 'number', min: 1 }]}>
              <InputNumber disabled={readOnly} min={1} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item name="contentType" label="发布类型">
            <Radio.Group
              disabled={readOnly}
              optionType="button"
              buttonStyle="solid"
              options={[
                { label: '图文公告（Markdown）', value: 'markdown' },
                { label: 'PDF 赛事文件', value: 'pdf' },
              ]}
              onChange={(event) => void changeContentType(event.target.value)}
            />
          </Form.Item>

          {contentType === 'markdown' ? (
            <Form.Item
              name="content"
              label="公告正文"
              rules={[
                { required: true, whitespace: true, message: '请输入公告正文' },
                { max: 100000, message: 'Markdown 正文最多 100,000 字符' },
                { validator: (_, value) => validateMarkdownSource(value) },
              ]}
            >
              {readOnly ? (
                <MdPreview
                  value={readOnlyMarkdown}
                  sanitize={sanitizeNoticeHtml}
                />
              ) : (
                <MdEditor
                  language="zh-CN"
                  value={form.getFieldValue('content') || ''}
                  onChange={(value) => form.setFieldValue('content', value)}
                  onUploadImg={uploadImages}
                  transformImgUrl={resolveAssetUrl}
                  sanitize={sanitizeNoticeHtml}
                  noMermaid
                  noKatex
                  style={{ height: 560 }}
                />
              )}
            </Form.Item>
          ) : (
            <>
              <Form.Item
                name="content"
                label="文件简介（选填）"
                rules={[{ max: 2000, message: 'PDF 简介最多 2,000 字符' }]}
              >
                <Input.TextArea disabled={readOnly} rows={5} maxLength={2000} showCount placeholder="请输入赛事文件简介" />
              </Form.Item>
              <div className="competition-notice-pdf">
                {pdfAsset ? (
                  <Descriptions bordered size="small" column={1}>
                    <Descriptions.Item label="PDF 文件">
                      <Space wrap>
                        <FilePdfOutlined className="competition-notice-pdf__icon" />
                        <span>{pdfAsset.originalName}</span>
                        <Tag>{fileSize(pdfAsset.sizeBytes)}</Tag>
                        <Button size="small" icon={<EyeOutlined />} loading={previewing} onClick={() => void openPdf(false)}>预览</Button>
                        <Button size="small" icon={<DownloadOutlined />} loading={previewing} onClick={() => void openPdf(true)}>下载</Button>
                        {!readOnly && <Button size="small" danger icon={<DeleteOutlined />} onClick={() => void removeAsset(pdfAsset)}>删除</Button>}
                      </Space>
                    </Descriptions.Item>
                  </Descriptions>
                ) : readOnly ? (
                  <Alert type="warning" showIcon message="当前公告未绑定 PDF 文件" />
                ) : (
                  <Upload.Dragger
                    accept=".pdf,application/pdf"
                    multiple={false}
                    showUploadList={false}
                    beforeUpload={uploadPdf}
                  >
                    <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                    <p className="ant-upload-text">点击或拖拽 PDF 到此处上传</p>
                    <p className="ant-upload-hint">仅支持 1 个 PDF，最大 50MB；再次上传将替换原文件</p>
                  </Upload.Dragger>
                )}
                {pdfAsset && !readOnly && (
                  <Upload accept=".pdf,application/pdf" showUploadList={false} beforeUpload={uploadPdf}>
                    <Button className="competition-notice-pdf__replace" loading={uploading}>替换 PDF</Button>
                  </Upload>
                )}
              </div>
            </>
          )}
        </Form>
      </Spin>
    </Drawer>
  );
}

export { contentTypeMeta };
