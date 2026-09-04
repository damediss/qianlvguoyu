import { Button, Checkbox, Empty, Space, Typography, Upload } from 'antd';
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
  replaceLoading?: boolean;
};

export function MaterialTreePanel({
  groups,
  selectedInstanceId,
  exportSelectedIds,
  onSelectInstance,
  onToggleExport,
  onToggleAllExport,
  onReplaceFile,
  replaceLoading,
}: Props) {
  const allIds = groups.flatMap((g) => g.items.map((i) => String(i.id)));
  const allChecked = allIds.length > 0 && allIds.every((id) => exportSelectedIds.has(id));
  const indeterminate = exportSelectedIds.size > 0 && !allChecked;

  if (!groups.length) {
    return <Empty description="暂无材料项，请确认配方已匹配" />;
  }

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
                    {item.hasFile ? <Typography.Text type="secondary" style={{ fontSize: 11 }}>有文件</Typography.Text> : null}
                  </Space>
                </button>
                <Upload
                  showUploadList={false}
                  openFileDialogOnClick
                  beforeUpload={(file) => {
                    onReplaceFile(id, file as File);
                    return false;
                  }}
                >
                  {/* 勿在子按钮上 stopPropagation：Upload 的选文件 click 挂在外层 span，拦截后会无反应 */}
                  <Button size="small" type="link" loading={replaceLoading && active}>
                    替换
                  </Button>
                </Upload>
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
