import { Button, Checkbox, Empty, Modal, Space, Typography, Upload, message } from 'antd';
import type { SubsidyMaterialInstance } from '../../../../api/training';
import { MaterialStatusTag } from './MaterialStatusTag';

type Props = {
  groups: Array<{ materialType: string; label: string; count: number; items: SubsidyMaterialInstance[] }>;
  selectedInstanceId: string | null;
  exportSelectedIds: Set<string>;
  onSelectInstance: (id: string) => void;
  onToggleExport: (id: string, checked: boolean) => void;
  onToggleAllExport: (checked: boolean, ids: string[]) => void;
  onReplaceFile: (instanceId: string, file: File) => void;
  onDeleteInstance?: (instanceId: string) => void | Promise<void>;
  replaceLoading?: boolean;
  deleteLoadingId?: string | null;
};

export function MaterialTreePanel({
  groups,
  selectedInstanceId,
  exportSelectedIds,
  onSelectInstance,
  onToggleExport,
  onToggleAllExport,
  onReplaceFile,
  onDeleteInstance,
  replaceLoading,
  deleteLoadingId,
}: Props) {
  const allIds = groups.flatMap((g) => g.items.map((i) => String(i.id)));
  const allChecked = allIds.length > 0 && allIds.every((id) => exportSelectedIds.has(id));
  const indeterminate = exportSelectedIds.size > 0 && !allChecked;

  if (!groups.length) {
    return <Empty description="暂无材料项，请确认配方已匹配" />;
  }

  const confirmDelete = (item: SubsidyMaterialInstance) => {
    if (!onDeleteInstance) return;
    const id = String(item.id);
    Modal.confirm({
      title: '删除该材料？',
      content: item.required
        ? `「${item.title}」为配方必填项，删除后将从本班清单中移除（可之后用模板填数等方式再补回）。`
        : `确认删除「${item.title}」？删除后不可在清单中恢复该条（历史版本一并不再展示）。`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await onDeleteInstance(id);
          message.success('已删除');
        } catch (e) {
          message.error((e as Error).message || '删除失败');
          throw e;
        }
      },
    });
  };

  return (
    <div className="material-tree-panel">
      <div className="material-tree-toolbar">
        <Checkbox
          checked={allChecked}
          indeterminate={indeterminate}
          onChange={(e) => onToggleAllExport(e.target.checked, allIds)}
        >
          全选导出
        </Checkbox>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          已选 {exportSelectedIds.size}/{allIds.length}
        </Typography.Text>
      </div>
      <Typography.Paragraph type="secondary" style={{ fontSize: 12, margin: '0 0 8px' }}>
        默认仅同步培训照片与证书材料；申请表 / 花名册 / 汇总表请用上方「从模板生成」或「替换上传」。
      </Typography.Paragraph>
      {groups.map((group) => (
        <div key={group.materialType} className="material-tree-group">
          <div className="material-tree-group-title">
            {group.label} ({group.count})
          </div>
          {group.items.map((item) => {
            const id = String(item.id);
            const active = selectedInstanceId === id;
            return (
              <div
                key={id}
                className={`material-tree-item${active ? ' material-tree-item--active' : ''}`}
              >
                <Checkbox
                  checked={exportSelectedIds.has(id)}
                  onChange={(e) => {
                    e.stopPropagation();
                    onToggleExport(id, e.target.checked);
                  }}
                  onClick={(e) => e.stopPropagation()}
                />
                <button type="button" className="material-tree-item-body" onClick={() => onSelectInstance(id)}>
                  <div className="material-tree-item-title">
                    {item.required ? <span className="material-required-dot">*</span> : null}
                    {item.title}
                  </div>
                  <Space size={4} wrap>
                    <MaterialStatusTag status={item.status} />
                    {item.hasFile ? (
                      <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                        有文件
                      </Typography.Text>
                    ) : null}
                  </Space>
                </button>
                <div className="material-tree-item-actions">
                  <Upload
                    showUploadList={false}
                    openFileDialogOnClick
                    beforeUpload={(file) => {
                      onReplaceFile(id, file as File);
                      return false;
                    }}
                  >
                    <Button size="small" type="link" loading={replaceLoading && active}>
                      替换
                    </Button>
                  </Upload>
                  {onDeleteInstance ? (
                    <Button
                      size="small"
                      type="link"
                      danger
                      loading={deleteLoadingId === id}
                      onClick={(e) => {
                        e.stopPropagation();
                        confirmDelete(item);
                      }}
                    >
                      删除
                    </Button>
                  ) : null}
                </div>
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
