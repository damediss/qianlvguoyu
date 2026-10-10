import type { FormalExamPlayerOption } from '../../api/formalExamPlayer';
import { FormalExamStem } from './FormalExamStem';

interface Props {
  multiple: boolean;
  options: FormalExamPlayerOption[];
  selectedOptionIds: string[];
  disabled?: boolean;
  onChange: (selectedOptionIds: string[]) => void;
}

export function FormalExamChoiceOptions({
  multiple,
  options,
  selectedOptionIds,
  disabled,
  onChange,
}: Props) {
  const selected = new Set(selectedOptionIds);
  return (
    <div className="formal-exam-options" role="group">
      {options.map((option) => {
        const checked = selected.has(option.optionId);
        return (
          <button
            key={option.optionId}
            type="button"
            className={`formal-exam-option${checked ? ' is-selected' : ''}`}
            disabled={disabled}
            aria-pressed={checked}
            onClick={() => {
              if (disabled) return;
              if (!multiple) {
                onChange(checked ? [] : [option.optionId]);
                return;
              }
              const next = new Set(selected);
              if (checked) next.delete(option.optionId);
              else next.add(option.optionId);
              onChange([...next]);
            }}
          >
            <span className="formal-exam-option__mark">{checked ? '●' : '○'}</span>
            <span className="formal-exam-option__label">{option.label}.</span>
            <FormalExamStem html={option.content} />
          </button>
        );
      })}
    </div>
  );
}
