export interface FormalExamLocalAnswer {
  selectedOptionIds?: string[];
  blanks?: string[];
}

export type FormalExamSaveStatus =
  | 'idle'
  | 'saving'
  | 'pending'
  | 'syncing'
  | 'saved'
  | 'error'
  | 'conflict';

export interface FormalExamQuestionState {
  answer: FormalExamLocalAnswer;
  marked: boolean;
  version: number;
  requestedVersion: number;
  saveStatus: FormalExamSaveStatus;
  saveError?: string;
}

export function isAnsweredLocal(type: string, answer: FormalExamLocalAnswer) {
  if (type === 'fill') {
    return Boolean(answer.blanks?.some((item) => item && item.trim()));
  }
  return (answer.selectedOptionIds || []).length > 0;
}
