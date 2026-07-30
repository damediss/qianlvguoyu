export type KnowledgeScopeType = 'enterprise' | 'department' | 'course';
export type KnowledgeAccessScope = 'inherit' | 'restricted';
export type SensitiveLevel = 'normal' | 'internal' | 'confidential';

export interface KnowledgeSpace {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  scopeType: KnowledgeScopeType | string;
  ownerUserId?: number;
  categoryCode?: string;
  description?: string;
  status: string;
  visibleRoleCodes: string[];
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocument {
  id: number;
  tenantId: number;
  spaceId: number;
  fileId: number;
  title: string;
  categoryCode?: string;
  sourceType: string;
  accessScope: KnowledgeAccessScope | string;
  sensitiveLevel: SensitiveLevel | string;
  parserStatus: string;
  indexStatus: string;
  chunkCount: number;
  textSha256?: string;
  errorMessage?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeSearchResult {
  chunkId: number;
  spaceId: number;
  spaceName: string;
  documentId: number;
  documentTitle: string;
  heading?: string;
  pageNo?: number;
  content: string;
  score: number;
  citationLabel: string;
}

export interface KnowledgeAnswerDraft {
  draftId: number;
  answer: string;
  evidenceSufficient: boolean;
  modelName: string;
  citations: KnowledgeSearchResult[];
  createdAt: string;
}

export interface FileVO {
  id: string;
  originalName: string;
  contentType?: string;
  sizeBytes: number;
  accessPolicy: string;
  status: string;
  createdAt: string;
}
