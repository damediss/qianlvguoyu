import type {
  CandidateCollectionContext,
  CandidateCollectionDetailVO,
  CollectionEntrySource,
} from '../types/certificationPracticalCollection';

export function normalizeActiveCollectionCandidate(
  candidate: CandidateCollectionDetailVO,
): CandidateCollectionDetailVO {
  if (candidate.overallStatus === 'COMPLETED') {
    return candidate;
  }
  return {
    ...candidate,
    overallStatus: 'COLLECTING',
    overallStatusLabel: '当前',
    queueStatus: 'COLLECTING',
    queueStatusLabel: '当前',
  };
}

export function hasCollectedCollectionData(candidate: CandidateCollectionDetailVO) {
  return Boolean(
    candidate.latestFingerprint?.fingerprintImageFileId
    || candidate.latestSignature?.signatureFileId,
  );
}

export function toCandidateCollectionContext(
  candidate: CandidateCollectionDetailVO,
  entrySource: CollectionEntrySource,
): CandidateCollectionContext {
  return {
    taskId: candidate.taskId,
    examSessionId: candidate.examSessionId,
    candidateId: candidate.candidateId,
    candidateName: candidate.name,
    idCardNo: candidate.maskedIdCardNo,
    collectionStatus: candidate.overallStatus,
    entrySource,
  };
}
