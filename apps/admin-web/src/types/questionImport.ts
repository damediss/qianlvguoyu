/**
 * 试题导入格式报告（方案 §14.4.3 / §14.4.4）。
 * 两条导入链共用；前端不得再按 number 读取导入结果。
 */
export interface QuestionImportIssue {
  severity: 'error' | 'warning';
  ruleCode: string;
  sheet: string;
  row: number;
  column: number;
  field: string;
  rawValue: string;
  matchedText: string;
  message: string;
}

export interface QuestionImportValidateResult {
  fileSha256: string;
  totalRows: number;
  validRows: number;
  hardErrorCount: number;
  warningCount: number;
  errors: QuestionImportIssue[];
  warnings: QuestionImportIssue[];
  truncated: boolean;
}

export interface QuestionImportResult {
  fileSha256: string;
  importedCount: number;
  warningCount: number;
  warnings: QuestionImportIssue[];
  truncated: boolean;
}
