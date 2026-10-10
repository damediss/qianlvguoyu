import { Input } from 'antd';

interface Props {
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
  onCommit: () => void;
}

export function FormalExamFillInput({ value, disabled, onChange, onCommit }: Props) {
  return (
    <Input.TextArea
      className="formal-exam-fill"
      value={value}
      disabled={disabled}
      autoSize={{ minRows: 3, maxRows: 8 }}
      placeholder="请输入答案"
      onChange={(event) => onChange(event.target.value)}
      onFocus={(event) => {
        event.target.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
      }}
      onBlur={onCommit}
    />
  );
}
