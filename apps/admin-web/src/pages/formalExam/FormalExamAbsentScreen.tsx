import type { FormalExamPlayerContext } from '../../api/formalExamPlayer';

function examTypeLabel(value?: string | null) {
  if (value === 'theory') return '理论考试';
  if (value === 'practical') return '实操考试';
  return value || '考试';
}

export function FormalExamAbsentScreen({
  context,
}: {
  context: FormalExamPlayerContext;
}) {
  const candidate = context.candidateExam;
  return (
    <div className="formal-exam-player">
      <div className="formal-exam-player__card formal-exam-absent">
        <h1 className="formal-exam-player__title">{examTypeLabel(context.examSession.examType)}</h1>
        <p className="formal-exam-player__status">本场考试已结束</p>
        <p className="formal-exam-player__subtitle">您未在规定时间内进入考试，</p>
        <p className="formal-exam-player__subtitle">系统已记录为缺考。</p>
        <div className="formal-exam-player__meta">
          <p>姓名：{candidate.candidateName || '-'}</p>
          <p>选手编号：{candidate.candidateNo || '-'}</p>
        </div>
        <p className="formal-exam-player__hint">如有疑问，请联系现场工作人员。</p>
      </div>
    </div>
  );
}
