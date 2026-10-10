import { Alert, List, Tag, Typography } from 'antd';
import type { StageValidationIssue } from '../../../types/competitionStagePlanning';

interface StageValidationPanelProps {
  issues: StageValidationIssue[];
  onSelectIssue: (issue: StageValidationIssue) => void;
}

export function StageValidationPanel({ issues, onSelectIssue }: StageValidationPanelProps) {
  const errors = issues.filter((issue) => issue.level === 'error');
  const warnings = issues.filter((issue) => issue.level === 'warning');
  return (
    <div className="stage-validation-panel" data-testid="stage-validation-panel">
      <div className="stage-validation-panel__summary">
        <SpaceSummary errors={errors.length} warnings={warnings.length} />
      </div>
      {issues.length === 0 ? (
        <Alert type="success" showIcon message="未发现编排问题" data-testid="stage-validation-success" />
      ) : (
        <List
          size="small"
          dataSource={issues}
          renderItem={(issue) => (
            <List.Item>
              <button type="button" className="stage-validation-item" onClick={() => onSelectIssue(issue)} data-testid={`stage-validation-${issue.code}`}>
                <Tag color={issue.level === 'error' ? 'error' : 'warning'}>{issue.level === 'error' ? '阻断' : '警告'}</Tag>
                <span>{issue.message}</span>
              </button>
            </List.Item>
          )}
        />
      )}
    </div>
  );
}

function SpaceSummary({ errors, warnings }: { errors: number; warnings: number }) {
  return <Typography.Text type="secondary">阻断 {errors} 项 · 警告 {warnings} 项</Typography.Text>;
}
