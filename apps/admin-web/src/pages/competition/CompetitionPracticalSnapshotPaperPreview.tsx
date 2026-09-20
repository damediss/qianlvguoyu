import type { ReactNode } from 'react';
import type {
  CompetitionMakeExamSnapshot,
  CompetitionMakeExamSnapshotQuestion,
} from '../../types/competitionQuestion';
import './CompetitionPracticalSnapshotPaperPreview.css';

type JsonRecord = Record<string, unknown>;

const DEFAULT_NEGATIVE_ITEMS_HTML = `
  <ol>
    <li>考生没有按规定穿戴劳保用品。</li>
    <li>操作过程中出现严重违章操作。</li>
    <li>造成人身伤害或设备损坏。</li>
  </ol>
`;

const PAPER_ALLOWED_TAGS = new Set([
  'P', 'BR', 'STRONG', 'EM', 'S', 'U', 'BLOCKQUOTE', 'UL', 'OL', 'LI',
  'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'PRE', 'CODE', 'TABLE', 'THEAD',
  'TBODY', 'TR', 'TH', 'TD', 'A', 'SPAN', 'DIV', 'SUP', 'SUB',
]);

function sanitizePaperHtml(html: string): string {
  if (typeof DOMParser === 'undefined') return '';
  const document = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html');
  const root = document.body.firstElementChild;
  if (!root) return '';

  const visit = (parent: Element) => {
    for (const node of Array.from(parent.childNodes)) {
      if (node.nodeType !== 1) continue;
      const child = node as Element;
      const tagName = child.tagName.toUpperCase();
      if (!PAPER_ALLOWED_TAGS.has(tagName)) {
        if (['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED', 'SVG', 'MATH'].includes(tagName)) {
          child.remove();
          continue;
        }
        visit(child);
        while (child.firstChild) parent.insertBefore(child.firstChild, child);
        child.remove();
        continue;
      }
      for (const attribute of Array.from(child.attributes)) {
        const name = attribute.name.toLowerCase();
        const allowed = (tagName === 'A' && ['href', 'title', 'target', 'rel'].includes(name))
          || ['class', 'colspan', 'rowspan'].includes(name);
        if (!allowed || (name === 'href' && !/^(https?:|mailto:)/i.test(attribute.value))) {
          child.removeAttribute(attribute.name);
        }
      }
      if (tagName === 'A') {
        child.setAttribute('target', '_blank');
        child.setAttribute('rel', 'noopener noreferrer');
      }
      visit(child);
    }
  };
  visit(root);
  return root.innerHTML;
}

function parseJson(value?: string | null): unknown {
  if (!value?.trim()) return undefined;
  try {
    return JSON.parse(value);
  } catch {
    return undefined;
  }
}

function asRecord(value: unknown): JsonRecord | undefined {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as JsonRecord
    : undefined;
}

function asRecords(value: unknown): JsonRecord[] {
  return Array.isArray(value) ? value.map(asRecord).filter((item): item is JsonRecord => Boolean(item)) : [];
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function firstContent(...values: unknown[]): string {
  for (const value of values) {
    const text = stringValue(value);
    if (text) return text;
  }
  return '';
}

function stripHtml(value: string): string {
  return value
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function formatNumber(value: unknown): string {
  if (value == null || value === '') return '-';
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return String(value);
  return numeric.toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1');
}

function formatDuration(value: unknown, fallback: number): string {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0 ? String(Math.round(numeric)) : String(fallback);
}

function PaperRichText({ value, className = '' }: { value?: string | null; className?: string }) {
  const content = stringValue(value);
  if (!content) return null;
  if (!/<[a-z][\s\S]*>/i.test(content)) {
    return (
      <div className={`competition-paper-preview__rich-text ${className}`.trim()}>
        {content.split(/\r?\n/).map((line, index) => <div key={`${line}-${index}`}>{line || '\u00a0'}</div>)}
      </div>
    );
  }
  return (
    <div
      className={`competition-paper-preview__rich-text ${className}`.trim()}
      dangerouslySetInnerHTML={{ __html: sanitizePaperHtml(content) }}
    />
  );
}

function PracticalQuestionTitle({ question, index }: { question: CompetitionMakeExamSnapshotQuestion; index: number }) {
  const questionTitle = stripHtml(question.stem || '') || '实操题';
  return <>{`试题${question.sortOrder || index + 1}、${questionTitle}`}</>;
}

interface PracticalQuestionPaperData {
  assessmentRequirementsHtml: string;
  candidatePrepareHtml: string;
  examRoomPrepareHtml: string;
  negativeItemsHtml: string;
  questionMaterialsHtml: string;
  durationMinutes: string;
  score: string;
}

function getPracticalQuestionPaperData(
  question: CompetitionMakeExamSnapshotQuestion,
  snapshotDurationMinutes: number,
): PracticalQuestionPaperData {
  const detail = asRecord(parseJson(question.practicalDetailJson));
  const contents = asRecords(detail?.assessmentContents);
  const contentHtml = contents
    .map((content) => stringValue(content.contentHtml))
    .filter(Boolean)
    .join('<p></p>');
  const totalScore = detail?.totalScore ?? question.score;

  return {
    assessmentRequirementsHtml: firstContent(
      detail?.assessmentRequirementsHtml,
      question.operationContent,
      contentHtml,
    ),
    candidatePrepareHtml: firstContent(detail?.candidatePrepareHtml, question.studentPrepare),
    examRoomPrepareHtml: firstContent(detail?.examRoomPrepareHtml, question.equipmentPrepare),
    negativeItemsHtml: firstContent(detail?.negativeItemsHtml) || DEFAULT_NEGATIVE_ITEMS_HTML,
    questionMaterialsHtml: firstContent(detail?.questionMaterialsHtml),
    durationMinutes: formatDuration(detail?.durationMinutes, snapshotDurationMinutes),
    score: formatNumber(question.score ?? totalScore),
  };
}

function PaperDetailLine({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="competition-paper-preview__detail-line">
      <span className="competition-paper-preview__detail-label">{label}</span>
      <div className="competition-paper-preview__detail-value">{children}</div>
    </div>
  );
}

function PaperSupportingInfo({ label, value }: { label: string; value: string }) {
  if (!value) return null;
  return (
    <div className="competition-paper-preview__supporting-info">
      <span className="competition-paper-preview__supporting-label">{label}：</span>
      <PaperRichText value={value} />
    </div>
  );
}

function PracticalSnapshotQuestion({
  question,
  index,
  snapshotDurationMinutes,
}: {
  question: CompetitionMakeExamSnapshotQuestion;
  index: number;
  snapshotDurationMinutes: number;
}) {
  const data = getPracticalQuestionPaperData(question, snapshotDurationMinutes);
  const negativeText = stripHtml(data.negativeItemsHtml);
  const negativeHasExplanation = /否定项说明|若考生|零分/.test(negativeText);

  return (
    <section className="competition-paper-preview__question">
      <h3 className="competition-paper-preview__question-title">
        <PracticalQuestionTitle question={question} index={index} />
      </h3>
      <p className="competition-paper-preview__line">（1）本题分值：<strong>{data.score}分</strong></p>
      <p className="competition-paper-preview__line">（2）考核时间：{data.durationMinutes}min</p>
      <p className="competition-paper-preview__line">（3）考核方式：模拟操作</p>
      <PaperDetailLine label="（4）具体考核要求：">
        {data.assessmentRequirementsHtml
          ? <PaperRichText value={data.assessmentRequirementsHtml} />
          : <span className="competition-paper-preview__placeholder">未配置</span>}
      </PaperDetailLine>
      <PaperSupportingInfo label="考场准备" value={data.examRoomPrepareHtml} />
      <PaperSupportingInfo label="考生准备" value={data.candidatePrepareHtml} />
      <PaperSupportingInfo label="试题材料" value={data.questionMaterialsHtml} />
      <PaperDetailLine label="（5）否定项说明：">
        {negativeHasExplanation && <PaperRichText value={data.negativeItemsHtml} />}
        {!negativeHasExplanation && (
          <>
            <p>若考生发生下列情况之一应及时终止其考试，考生该考试记为零分。</p>
            <PaperRichText value={data.negativeItemsHtml} className="competition-paper-preview__negative-list" />
          </>
        )}
      </PaperDetailLine>
    </section>
  );
}

export function CompetitionPracticalSnapshotPaperPreview({ snapshot }: { snapshot: CompetitionMakeExamSnapshot }) {
  const questions = [...(snapshot.questions || [])].sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0));

  return (
    <div className="competition-paper-preview" data-testid="competition-practical-snapshot-paper">
      <article className="competition-paper-preview__sheet">
        <header className="competition-paper-preview__header">
          <h1>职业技能等级认定</h1>
          <h2>{snapshot.title || '技能现场试卷考核试卷'}</h2>
          <div className="competition-paper-preview__candidate-fields" aria-label="考生信息">
            <span>考件编号：<i aria-hidden="true" /></span>
            <span>姓名：<i aria-hidden="true" /></span>
            <span>准考证号：<i aria-hidden="true" /></span>
          </div>
        </header>

        <section className="competition-paper-preview__notice">
          <h2>注 意 事 项</h2>
          <p>一、请根据试题考核要求，完成考试内容。</p>
          <p>二、请服从考评人员指挥，保证考核安全顺利进行。</p>
        </section>

        {questions.length > 0 ? questions.map((question, index) => (
          <PracticalSnapshotQuestion
            key={question.id}
            question={question}
            index={index}
            snapshotDurationMinutes={snapshot.durationMinutes}
          />
        )) : (
          <p className="competition-paper-preview__empty">暂无题目内容</p>
        )}
      </article>
    </div>
  );
}
