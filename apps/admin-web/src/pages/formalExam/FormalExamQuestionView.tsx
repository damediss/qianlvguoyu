import type { FormalExamPlayerQuestion } from '../../api/formalExamPlayer';
import { FormalExamChoiceOptions } from './FormalExamChoiceOptions';
import { FormalExamFillInput } from './FormalExamFillInput';
import { FormalExamStem } from './FormalExamStem';
import type { FormalExamLocalAnswer } from './formalExamAnswerState';

interface Props {
  question: FormalExamPlayerQuestion;
  answer: FormalExamLocalAnswer;
  disabled?: boolean;
  onChoiceChange: (selectedOptionIds: string[]) => void;
  onFillChange: (text: string) => void;
  onFillCommit: () => void;
}

export function FormalExamQuestionView({
  question,
  answer,
  disabled,
  onChoiceChange,
  onFillChange,
  onFillCommit,
}: Props) {
  const type = question.type;
  return (
    <div className="formal-exam-question">
      <div className="formal-exam-question__title">
        <span>{question.order}.</span>
        <FormalExamStem html={question.stem} />
      </div>
      {type === 'fill' ? (
        <FormalExamFillInput
          value={answer.blanks?.[0] || ''}
          disabled={disabled}
          onChange={onFillChange}
          onCommit={onFillCommit}
        />
      ) : (
        <FormalExamChoiceOptions
          multiple={type === 'multi'}
          options={question.options || []}
          selectedOptionIds={answer.selectedOptionIds || []}
          disabled={disabled}
          onChange={onChoiceChange}
        />
      )}
    </div>
  );
}
