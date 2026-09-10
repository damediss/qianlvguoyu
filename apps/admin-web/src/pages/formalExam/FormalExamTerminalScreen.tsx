import { useEffect, useState } from 'react';
import type { FormalExamPlayerContext } from '../../api/formalExamPlayer';
import { formalExamLocalAnswerRepository } from './idb/FormalExamLocalAnswerRepository';
import { formatSubmitClock } from './formalExamTime';

function examTypeLabel(value?: string | null) {
  if (value === 'theory') return '理论考试';
  if (value === 'practical') return '实操考试';
  return value || '考试';
}

function copyOf(status?: string | null) {
  if (status === 'auto_submitted') {
    return {
      headline: '考试时间已到',
      detail: '系统已自动提交试卷',
      hint: '请等待现场工作人员下一步安排',
    };
  }
  if (status === 'forced_submitted') {
    return {
      headline: '本场考试已由工作人员结束提交',
      detail: null,
      hint: '请联系现场工作人员',
    };
  }
  return {
    headline: '试卷提交成功',
    detail: null,
    hint: '请等待现场工作人员下一步安排',
  };
}

export function FormalExamTerminalScreen({
  context,
}: {
  context: FormalExamPlayerContext;
}) {
  const candidate = context.candidateExam;
  const copy = copyOf(candidate.status);
  const [unsyncedCount, setUnsyncedCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    void formalExamLocalAnswerRepository
      .countUnsynced(String(context.examSession.id), String(candidate.id))
      .then((count) => {
        if (!cancelled) setUnsyncedCount(count);
      })
      .catch(() => {
        if (!cancelled) setUnsyncedCount(0);
      });
    return () => {
      cancelled = true;
    };
  }, [candidate.id, context.examSession.id]);

  return (
    <div className="formal-exam-player">
      <div className="formal-exam-player__card formal-exam-submitted">
        <h1 className="formal-exam-player__title">{examTypeLabel(context.examSession.examType)}</h1>
        <p className="formal-exam-submitted__mark" aria-hidden="true">✓</p>
        <p className="formal-exam-player__status">{copy.headline}</p>
        {copy.detail ? <p className="formal-exam-player__subtitle">{copy.detail}</p> : null}
        <div className="formal-exam-player__meta">
          <p>姓名：{candidate.candidateName || '-'}</p>
          <p>选手编号：{candidate.candidateNo || '-'}</p>
          <p>提交时间：{formatSubmitClock(candidate.submitTime)}</p>
        </div>
        {unsyncedCount > 0 ? (
          <p className="formal-exam-player__unsynced">
            本设备仍保留 {unsyncedCount} 道未同步的本地作答记录。
            这些记录未自动计入正式提交结果。
            请勿卸载或清除应用，并联系现场工作人员。
          </p>
        ) : null}
        <p className="formal-exam-player__hint">{copy.hint}</p>
      </div>
    </div>
  );
}
