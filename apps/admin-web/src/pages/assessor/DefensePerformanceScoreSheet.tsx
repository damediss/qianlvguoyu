import { Checkbox, Select } from 'antd';
import type { Ref } from 'react';
import { DefenseExpertSignature, type DefenseExpertSignatureHandle, type DefenseSignatureValue } from './DefenseExpertSignature';
import {
  PERFORMANCE_CATEGORIES,
  formatScore,
  performanceTotal,
  scoreOptions,
} from './defenseScoreCatalog';
import type { DefenseAssessorReportDetail, DefensePerformancePayload } from '../../types/defenseScoring';

interface Props {
  candidate?: DefenseAssessorReportDetail;
  assessorName?: string;
  taskId?: string;
  value: DefensePerformancePayload;
  disabled?: boolean;
  signature?: DefenseSignatureValue | null;
  signaturePreviewUrl?: string;
  signatureRef?: Ref<DefenseExpertSignatureHandle>;
  onChange: (next: DefensePerformancePayload) => void;
  onSignatureChange: (value: DefenseSignatureValue | null) => void;
}

function scoreMap(value: DefensePerformancePayload) {
  const map = new Map<string, number | null>();
  for (const item of value.items || []) {
    map.set(item.key, item.score ?? null);
  }
  return map;
}

export function emptyPerformancePayload(): DefensePerformancePayload {
  return {
    items: PERFORMANCE_CATEGORIES.flatMap((category) => category.items.map((item) => ({ key: item.key, score: null }))),
    disqualify: false,
    remark: '',
  };
}

export function DefensePerformanceScoreSheet({
  candidate,
  assessorName,
  taskId,
  value,
  disabled,
  signature,
  signaturePreviewUrl,
  signatureRef,
  onChange,
  onSignatureChange,
}: Props) {
  const scores = scoreMap(value);
  const total = performanceTotal(value.items);
  const occupation = candidate?.workTypeName || '—';
  const level = candidate?.levelName || '—';

  const setScore = (key: string, score: number | null) => {
    const nextItems = PERFORMANCE_CATEGORIES.flatMap((category) => category.items).map((item) => ({
      key: item.key,
      score: item.key === key ? score : (scores.get(item.key) ?? null),
    }));
    onChange({ ...value, items: nextItems });
  };

  return (
    <div className="defense-paper">
      <h2 className="defense-paper-title">技师、高级技师业绩评估报告评分表</h2>
      <p className="defense-paper-meta">
        申报职业（工种）：{occupation}
        <span className="defense-paper-meta-gap">级别：{level}</span>
      </p>
      <table className="defense-paper-table">
        <colgroup>
          <col style={{ width: '11%' }} />
          <col style={{ width: '16%' }} />
          <col style={{ width: '9%' }} />
          <col style={{ width: '14%' }} />
          <col style={{ width: '32%' }} />
          <col style={{ width: '18%' }} />
        </colgroup>
        <tbody>
          <tr>
            <th colSpan={2}>考生姓名</th>
            <td>{candidate?.candidateName || '—'}</td>
            <th>准考证号</th>
            <td colSpan={2} className="defense-paper-ticket">{candidate?.admissionTicketNo || '—'}</td>
          </tr>
          <tr>
            <th colSpan={2}>项 目</th>
            <th>配分</th>
            <th colSpan={2}>评 分 内 容</th>
            <th>评分结果</th>
          </tr>
          {PERFORMANCE_CATEGORIES.flatMap((category) => (
            category.items.map((item, index) => (
              <tr key={item.key}>
                {index === 0 ? (
                  <th rowSpan={category.items.length} className="defense-paper-category">
                    {category.label}
                    <span>（{category.maxScore}分）</span>
                  </th>
                ) : null}
                <td>{item.label}</td>
                <td className="defense-paper-center">{item.maxScore}</td>
                <td colSpan={2} className="defense-paper-criteria">
                  {item.criteria}
                  {item.disqualify ? (
                    <div className="defense-paper-disqualify">
                      <Checkbox
                        disabled={disabled}
                        checked={Boolean(value.disqualify)}
                        onChange={(event) => onChange({ ...value, disqualify: event.target.checked })}
                      >
                        不符合申报条件（否定项，综合科目不合格）
                      </Checkbox>
                    </div>
                  ) : null}
                </td>
                <td>
                  <Select
                    className="defense-paper-score"
                    disabled={disabled}
                    allowClear
                    placeholder="选择"
                    getPopupContainer={() => document.body}
                    value={scores.get(item.key) ?? undefined}
                    options={scoreOptions(item.maxScore).map((score) => ({ label: String(score), value: score }))}
                    onChange={(score) => setScore(item.key, typeof score === 'number' ? score : null)}
                  />
                </td>
              </tr>
            ))
          ))}
          <tr className={`defense-paper-subtotal${value.disqualify ? ' is-rejected' : ''}`}>
            <th colSpan={2}>合 计</th>
            <td className="defense-paper-center">100</td>
            <td colSpan={2} className="defense-paper-criteria">
              各项均考核任现职以来的情况，每项最高得分不得超过标准分。
              {value.disqualify ? ' 任职年限不符合申报条件，综合科目成绩判定不合格。' : ''}
            </td>
            <td className="defense-paper-center">{formatScore(total)}</td>
          </tr>
          <tr>
            <th>评审专家</th>
            <td colSpan={5}>
              <DefenseExpertSignature
                ref={signatureRef}
                taskId={taskId}
                signerName={assessorName}
                disabled={disabled}
                previewUrl={signaturePreviewUrl}
                value={signature}
                onChange={onSignatureChange}
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
