import type { CandidateCollectionDetailVO, SignerType, SnowflakeId } from '../../types/certificationPracticalCollection';

export interface SignatureCaptureResult {
  requestId: string;
  success: boolean;
  signatureFileId: SnowflakeId;
  signatureHash: string;
  signatureFormat?: string;
  signedAt: string;
  signerType: SignerType;
  signerName: string;
  deviceInfo?: Record<string, unknown>;
}

export interface SignatureAdapter {
  open(candidate: CandidateCollectionDetailVO): Promise<SignatureCaptureResult>;
}
