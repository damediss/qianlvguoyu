import { Drawer } from 'antd';
import type { FormalExamPlayerQuestion } from '../../api/formalExamPlayer';
import type { FormalExamQuestionState } from './formalExamAnswerState';
import { isAnsweredLocal } from './formalExamAnswerState';

interface Props {
  open: boolean;
  questions: FormalExamPlayerQuestion[];
  states: Record<string, FormalExamQuestionState>;
  currentId?: string;
  onClose: () => void;
  onJump: (questionSnapshotId: string) => void;
}

export function FormalExamAnswerCard({
  open,
  questions,
  states,
  currentId,
  onClose,
  onJump,
}: Props) {
  return (
    <Drawer
      title="答题卡"
      placement="bottom"
      height="72%"
      open={open}
      onClose={onClose}
      className="formal-exam-card-drawer"
      destroyOnHidden
    >
      <div className="formal-exam-card">
        {questions.map((question) => {
          const state = states[question.questionSnapshotId];
          const answered = state ? isAnsweredLocal(question.type, state.answer) : false;
          const marked = Boolean(state?.marked);
          const pending = answered && state?.saveStatus !== 'saved';
          const current = question.questionSnapshotId === currentId;
          const className = [
            'formal-exam-card__item',
            answered ? 'is-answered' : '',
            pending ? 'is-pending' : '',
            marked ? 'is-marked' : '',
            current ? 'is-current' : '',
          ].filter(Boolean).join(' ');
          return (
            <button
              key={question.questionSnapshotId}
              type="button"
              className={className}
              onClick={() => onJump(question.questionSnapshotId)}
            >
              {question.order}
              {marked ? <span className="formal-exam-card__flag">标</span> : null}
            </button>
          );
        })}
      </div>
    </Drawer>
  );
}
