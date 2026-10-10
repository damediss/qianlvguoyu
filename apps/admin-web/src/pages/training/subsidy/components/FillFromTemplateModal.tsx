import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Modal, Radio, Space, Table, Tag, Typography, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import * as trainApi from '../../../../api/training';
import type { SubsidyMaterialTemplate } from '../../../../api/training';

type Props = {
  open: boolean;
  onClose: () => void;
  subsidyId: number | string;
  classId?: number | string | null;
  instanceId?: number | string | null;
  instanceTitle?: string | null;
  instanceHasFile?: boolean;
  instanceFileName?: string | null;
  hasUploadTemplate?: boolean;
  uploadTemplateFileName?: string | null;
  materialType?: string | null;
  onFilled?: (instanceId?: number | string) => void;
};

type FillSource = 'current' | 'library';

function canAutoFill(fmt?: string) {
  const f = (fmt || '').toLowerCase();
  return f === 'docx' || f === 'xlsx' || f === 'xls';
}

function canAutoFillFileName(name?: string | null) {
  const n = (name || '').toLowerCase();
  return n.endsWith('.docx') || n.endsWith('.xlsx') || n.endsWith('.xls');
}

export function FillFromTemplateModal({
  open,
  onClose,
  subsidyId,
  classId,
  instanceId,
  instanceTitle,
  instanceHasFile,
  instanceFileName,
  hasUploadTemplate,
  uploadTemplateFileName,
  materialType,
  onFilled,
}: Props) {
  const [fillSource, setFillSource] = useState<FillSource>('library');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const previewLooksFilled =
    !!instanceFileName
    && (/^\[(填数|模板)\]/i.test(instanceFileName) || instanceFileName.includes('花名册'));

  const canUseCurrent =
    instanceId != null
    && (!!hasUploadTemplate
      || (!!instanceHasFile
        && !previewLooksFilled
        && (!instanceFileName || canAutoFillFileName(instanceFileName))));

  const fillBaseName = uploadTemplateFileName || (canUseCurrent ? instanceFileName : null);

  const listQ = useQuery({
    queryKey: ['subsidy-material-templates', 'fill', classId, materialType],
    queryFn: () =>
      trainApi.listSubsidyMaterialTemplates({
        classId: classId ?? undefined,
        status: 'active',
      }),
    enabled: open,
  });

  useEffect(() => {
    if (!open) {
      setSelectedId(null);
      setFillSource('library');
      return;
    }
    setFillSource(canUseCurrent ? 'current' : 'library');
  }, [open, canUseCurrent]);

  useEffect(() => {
    if (!open || fillSource !== 'library') return;
    const rows = listQ.data ?? [];
    const matchType = materialType
      ? rows.find((r) => canAutoFill(r.fileFormat) && r.materialType === materialType)
      : undefined;
    const prefer = matchType || rows.find((r) => canAutoFill(r.fileFormat));
    setSelectedId(prefer ? String(prefer.id) : null);
  }, [open, fillSource, listQ.data, materialType]);

  const rows = useMemo(() => {
    const all = listQ.data ?? [];
    if (!materialType) return all;
    const matched = all.filter((r) => r.materialType === materialType);
    const others = all.filter((r) => r.materialType !== materialType);
    return matched.length ? [...matched, ...others] : all;
  }, [listQ.data, materialType]);

  const selected = rows.find((r) => String(r.id) === selectedId) ?? null;

  const fillMu = useMutation({
    mutationFn: async () => {
      if (fillSource === 'current') {
        if (instanceId == null) throw new Error('缺少材料');
        return trainApi.fillSubsidyMaterialInstance(instanceId, {
          useCurrentFile: true,
          rosterScope: 'certified',
        });
      }
      if (!selectedId) throw new Error('请选择模板');
      const body = { templateId: selectedId, rosterScope: 'certified' as const };
      if (instanceId != null) {
        return trainApi.fillSubsidyMaterialInstance(instanceId, body);
      }
      return trainApi.fillSubsidyFromTemplate(subsidyId, body);
    },
    onSuccess: (res) => {
      if (res.missingKeys?.length) {
        message.warning(res.message || '已生成，部分字段为空');
      } else {
        message.success(res.message || '已按模板生成');
      }
      onFilled?.(res.instanceId ?? instanceId ?? undefined);
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '填数失败'),
  });

  const okDisabled =
    fillSource === 'current'
      ? !canUseCurrent
      : !selectedId || !canAutoFill(selected?.fileFormat);

  return (
    <Modal
      title={instanceId ? `从模板填数：${instanceTitle || '当前材料'}` : '选模板 → 自动填数 → 生成'}
      open={open}
      onCancel={onClose}
      width={800}
      destroyOnClose
      footer={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button
            type="primary"
            disabled={okDisabled}
            loading={fillMu.isPending}
            onClick={() => fillMu.mutate()}
          >
            {fillSource === 'current' ? '按上传模板填数' : '生成并填数'}
          </Button>
        </Space>
      }
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="申报补贴仅纳入已获证人员。无 ${} 占位符的申请表也会按「培训机构名称」等中文标签自动匹配填右侧空格；优先用最近一次「替换上传」底稿。"
      />

      {instanceId ? (
        <div style={{ marginBottom: 12 }}>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
            将写入当前材料的新版本（保留历史），不新建清单项。
          </Typography.Text>
          <Radio.Group
            value={fillSource}
            onChange={(e) => setFillSource(e.target.value as FillSource)}
            optionType="button"
            buttonStyle="solid"
          >
            <Radio.Button value="current" disabled={!canUseCurrent}>
              基于上传模板
            </Radio.Button>
            <Radio.Button value="library">从模板库选择</Radio.Button>
          </Radio.Group>
          {fillSource === 'current' ? (
            <Typography.Paragraph style={{ marginTop: 10, marginBottom: 0 }}>
              填数底稿：
              <Typography.Text code>{fillBaseName || '未知'}</Typography.Text>
              <br />
              <Typography.Text type="secondary">
                使用最近一次「替换上传」的文件写入本班数据（不是右侧预览里已生成的 [填数] 文件）。
              </Typography.Text>
            </Typography.Paragraph>
          ) : null}
          {previewLooksFilled && fillSource === 'current' ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginTop: 10 }}
              message="右侧预览文件名像填数结果，系统不会拿它当模板；请确认曾「替换上传」过正确的申请表空白模板。"
            />
          ) : null}
          {!canUseCurrent ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginTop: 10 }}
              message="尚无可用的上传模板。请先对当前材料「替换上传」正确的申请表（docx/xlsx），再回来填数；或改从模板库选择匹配类型的模板。"
            />
          ) : null}
        </div>
      ) : (
        <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
          将在本班材料清单中新增一项（类型取自模板）。
        </Typography.Paragraph>
      )}

      {fillSource === 'library' || !instanceId ? (
        <Table<SubsidyMaterialTemplate>
          rowKey={(r) => String(r.id)}
          size="small"
          loading={listQ.isLoading}
          dataSource={rows}
          pagination={false}
          scroll={{ y: 320 }}
          onRow={(row) => ({
            onClick: () => {
              if (canAutoFill(row.fileFormat)) setSelectedId(String(row.id));
            },
            style: {
              cursor: canAutoFill(row.fileFormat) ? 'pointer' : 'not-allowed',
              opacity: canAutoFill(row.fileFormat) ? 1 : 0.55,
            },
          })}
          columns={[
            {
              title: '',
              width: 48,
              render: (_: unknown, row) => (
                <Radio
                  disabled={!canAutoFill(row.fileFormat)}
                  checked={selectedId === String(row.id)}
                  onChange={() => setSelectedId(String(row.id))}
                />
              ),
            },
            { title: '标题', dataIndex: 'title', ellipsis: true },
            {
              title: '类型',
              dataIndex: 'materialTypeLabel',
              width: 88,
              render: (label: string, row) =>
                materialType && row.materialType === materialType ? (
                  <Tag color="blue">{label || row.materialType}</Tag>
                ) : (
                  label || row.materialType || '—'
                ),
            },
            {
              title: '格式',
              dataIndex: 'fileFormat',
              width: 72,
              render: (v: string) => <Tag color={canAutoFill(v) ? 'blue' : 'default'}>{v}</Tag>,
            },
            {
              title: '占位符',
              dataIndex: 'fieldKeys',
              ellipsis: true,
              render: (keys?: string[]) =>
                keys?.length ? keys.slice(0, 5).join(', ') + (keys.length > 5 ? '…' : '') : '—',
            },
          ]}
        />
      ) : null}

      {fillSource === 'library' && !rows.length && !listQ.isLoading ? (
        <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
          暂无模板，请先打开「模板库」上传 docx / xlsx 填数模板。
        </Typography.Paragraph>
      ) : null}
    </Modal>
  );
}
