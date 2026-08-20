import { Checkbox, Input, Select } from 'antd';
import type { Ref } from 'react';
import { DefenseExpertSignature, type DefenseExpertSignatureHandle, type DefenseSignatureValue } from './DefenseExpertSignature';
import {
  THESIS_CONTENT_ITEMS,
  THESIS_DEFENSE_ITEMS,
  THESIS_REJECT_ITEMS,
  formatScore,
  scoreOptions,
  sumScores,
  thesisFinalScore,
} from './defenseScoreCatalog';
import type { DefenseAssessorReportDetail, DefenseThesisPayload } from '../../types/defenseScoring';

interface Props {
  candidate?: DefenseAssessorReportDetail;
  assessorName?: string;
  taskId?: string;
  value: DefenseThesisPayload;
  disabled?: boolean;
  signature?: DefenseSignatureValue | null;
  signaturePreviewUrl?: string;
  signatureRef?: Ref<DefenseExpertSignatureHandle>;
  onChange: (next: DefenseThesisPayload) => void;
  onSignatureChange: (value: DefenseSignatureValue | null) => void;
}

function pad<T>(source: T[] | undefined, size: number, fallback: T): T[] {
  const next = Array.from({ length: size }, (_, index) => source?.[index] ?? fallback);
  return next;
}

export function emptyThesisPayload(): DefenseThesisPayload {
  return {
    paperTitle: '',
    rejectFlags: THESIS_REJECT_ITEMS.map(() => false),
    rejectNote: '',
    contentScores: THESIS_CONTENT_ITEMS.map(() => null),
    questions: ['', '', ''],
    defenseScores: THESIS_DEFENSE_ITEMS.map(() => null),
  };
}

export function DefenseThesisScoreSheet({
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
  const rejectFlags = pad(value.rejectFlags, THESIS_REJECT_ITEMS.length, false);
  const contentScores = pad(value.contentScores, THESIS_CONTENT_ITEMS.length, null);
  const questions = pad(value.questions, 3, '');
  const defenseScores = pad(value.defenseScores, THESIS_DEFENSE_ITEMS.length, null);
  const contentSubtotal = sumScores(contentScores);
  const defenseSubtotal = sumScores(defenseScores);
  const finalScore = thesisFinalScore(contentScores, defenseScores);
  const occupation = [candidate?.workTypeName, candidate?.levelName].filter(Boolean).join('  ');

  const patch = (partial: Partial<DefenseThesisPayload>) => {
    onChange({ ...value, ...partial });
  };

  return (
    <div className="defense-paper">
      <h2 className="defense-paper-title">技术总结（论文）答辩评审评分表</h2>
      <p className="defense-paper-meta">
        申报职业：{occupation || '—'}
      </p>
      <table className="defense-paper-table">
        <colgroup>
          <col style={{ width: '15%' }} />
          <col style={{ width: '22%' }} />
          <col style={{ width: '16%' }} />
          <col style={{ width: '47%' }} />
        </colgroup>
        <tbody>
          <tr>
            <th>考生姓名</th>
            <td>{candidate?.candidateName || '—'}</td>
            <th>准考证号</th>
            <td>{candidate?.admissionTicketNo || '—'}</td>
          </tr>
          <tr>
            <th>论文题目</th>
            <td colSpan={3}>
              <Input
                variant="borderless"
                disabled={disabled}
                placeholder="请输入论文题目"
                value={value.paperTitle || ''}
                onChange={(event) => patch({ paperTitle: event.target.value })}
              />
            </td>
          </tr>
          <tr>
            <th>否定项</th>
            <td colSpan={3}>
              <div className="defense-paper-rejects">
                {THESIS_REJECT_ITEMS.map((item, index) => (
                  <Checkbox
                    key={item}
                    disabled={disabled}
                    checked={Boolean(rejectFlags[index])}
                    onChange={(event) => {
                      const next = [...rejectFlags];
                      next[index] = event.target.checked;
                      patch({ rejectFlags: next });
                    }}
                  >
                    {index + 1}.{item}
                  </Checkbox>
                ))}
              </div>
            </td>
          </tr>
          <tr>
            <th>否定说明</th>
            <td colSpan={3}>
              <Input.TextArea
                autoSize={{ minRows: 2, maxRows: 4 }}
                disabled={disabled}
                placeholder="勾选否定项后请说明原因"
                value={value.rejectNote || ''}
                onChange={(event) => patch({ rejectNote: event.target.value })}
              />
            </td>
          </tr>
          <tr>
            <th rowSpan={THESIS_CONTENT_ITEMS.length + 2} className="defense-paper-section">
              论文内容部分
              <span>（40%）</span>
            </th>
            <th>评定项目</th>
            <th>满分</th>
            <th>实际成绩(分)</th>
          </tr>
          {THESIS_CONTENT_ITEMS.map((item, index) => (
            <tr key={item.key}>
              <td>{index + 1}.{item.label}</td>
              <td className="defense-paper-center">{item.maxScore}</td>
              <td>
                <Select
                  className="defense-paper-score"
                  disabled={disabled}
                  allowClear
                  placeholder="选择"
                  getPopupContainer={() => document.body}
                  value={contentScores[index] ?? undefined}
                  options={scoreOptions(item.maxScore).map((score) => ({ label: String(score), value: score }))}
                  onChange={(score) => {
                    const next = [...contentScores];
                    next[index] = typeof score === 'number' ? score : null;
                    patch({ contentScores: next });
                  }}
                />
              </td>
            </tr>
          ))}
          <tr className="defense-paper-subtotal">
            <th>小    计</th>
            <td className="defense-paper-center">100</td>
            <td className="defense-paper-center">{formatScore(contentSubtotal)}</td>
          </tr>
          <tr>
            <th rowSpan={THESIS_DEFENSE_ITEMS.length + 6} className="defense-paper-section">
              论文答辩部分
              <span>（60%）</span>
            </th>
            <th colSpan={3}>问题</th>
          </tr>
          {questions.map((question, index) => (
            <tr key={`q-${index}`}>
              <td colSpan={3}>
                <Input
                  variant="borderless"
                  disabled={disabled}
                  prefix={`${index + 1}.`}
                  placeholder="请输入答辩问题"
                  value={question}
                  onChange={(event) => {
                    const next = [...questions];
                    next[index] = event.target.value;
                    patch({ questions: next });
                  }}
                />
              </td>
            </tr>
          ))}
          <tr>
            <th>评定项目</th>
            <th>满分</th>
            <th>实际成绩(分)</th>
          </tr>
          {THESIS_DEFENSE_ITEMS.map((item, index) => (
            <tr key={item.key}>
              <td>{index + 1}.{item.label}</td>
              <td className="defense-paper-center">{item.maxScore}</td>
              <td>
                <Select
                  className="defense-paper-score"
                  disabled={disabled}
                  allowClear
                  placeholder="选择"
                  getPopupContainer={() => document.body}
                  value={defenseScores[index] ?? undefined}
                  options={scoreOptions(item.maxScore).map((score) => ({ label: String(score), value: score }))}
                  onChange={(score) => {
                    const next = [...defenseScores];
                    next[index] = typeof score === 'number' ? score : null;
                    patch({ defenseScores: next });
                  }}
                />
              </td>
            </tr>
          ))}
          <tr className="defense-paper-subtotal">
            <th>小    计</th>
            <td className="defense-paper-center">100</td>
            <td className="defense-paper-center">{formatScore(defenseSubtotal)}</td>
          </tr>
          <tr className="defense-paper-final">
            <th>最终得分</th>
            <td colSpan={2}>最终得分=论文内容部分（40%）+论文答辩部分（60%）</td>
            <td className="defense-paper-center">{formatScore(finalScore)}</td>
          </tr>
          <tr>
            <th>评审专家</th>
            <td colSpan={3}>
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
