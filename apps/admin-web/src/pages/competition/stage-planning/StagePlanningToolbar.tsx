import { Button, Space, Tag, Typography } from 'antd';
import { CheckCircleOutlined, CheckOutlined, ColumnWidthOutlined, RollbackOutlined, SaveOutlined } from '@ant-design/icons';

interface StagePlanningToolbarProps {
  editable: boolean;
  dirty: boolean;
  saving: boolean;
  errorCount: number;
  warningCount: number;
  onAutoLayout: () => void;
  onValidate: () => void;
  onSave: () => void;
  onDiscard: () => void;
  onFitView: () => void;
  onConfirm?: () => void;
  confirming?: boolean;
}

export function StagePlanningToolbar({ editable, dirty, saving, errorCount, warningCount, onAutoLayout, onValidate, onSave, onDiscard, onFitView, onConfirm, confirming = false }: StagePlanningToolbarProps) {
  return (
    <div className="stage-planning-toolbar" data-testid="stage-planning-toolbar">
      <div>
        <Typography.Title level={4}>赛段画布</Typography.Title>
        <Typography.Text type="secondary">业务顺序由固定赛段类型派生，画布连线仅用于本地编排说明。</Typography.Text>
      </div>
      <Space wrap>
        {dirty && <Tag color="orange" data-testid="stage-dirty-indicator">有未保存修改</Tag>}
        {errorCount > 0 && <Tag color="error">阻断 {errorCount}</Tag>}
        {warningCount > 0 && <Tag color="warning">警告 {warningCount}</Tag>}
        <Button icon={<ColumnWidthOutlined />} onClick={onFitView} data-testid="stage-fit-view">适配视口</Button>
        <Button disabled={!editable} onClick={onAutoLayout} data-testid="stage-auto-layout">自动排列</Button>
        <Button onClick={onValidate} icon={<CheckOutlined />} data-testid="stage-validate">校验编排</Button>
        {editable && dirty && <Button icon={<RollbackOutlined />} onClick={onDiscard} data-testid="stage-discard">放弃修改</Button>}
        <Button type="primary" icon={<SaveOutlined />} disabled={!editable || !dirty || errorCount > 0} loading={saving} onClick={onSave} data-testid="stage-save-draft">保存草稿</Button>
        {editable && onConfirm && <Button
          icon={<CheckCircleOutlined />}
          disabled={dirty || saving || errorCount > 0}
          loading={confirming}
          onClick={onConfirm}
          data-testid="stage-confirm"
        >确认编排</Button>}
      </Space>
    </div>
  );
}
