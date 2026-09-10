import { sanitizeExamHtml } from './sanitizeExamHtml';

export function FormalExamStem({ html }: { html?: string | null }) {
  const content = html?.trim() ? html : '';
  if (!content) return <p className="formal-exam-stem">-</p>;
  if (!/[<>]/.test(content)) {
    return <div className="formal-exam-stem">{content}</div>;
  }
  return (
    <div
      className="formal-exam-stem"
      dangerouslySetInnerHTML={{ __html: sanitizeExamHtml(content) }}
    />
  );
}
