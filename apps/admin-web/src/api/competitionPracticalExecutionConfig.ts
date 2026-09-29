import { apiClient, unwrap } from './client';
import type { CompetitionId } from '../types/competition';
import type {
  CompetitionPracticalExecutionConfig,
  CompetitionPracticalExecutionConfigRequest,
  CompetitionPracticalExecutionConfigValidation,
  CompetitionPracticalScoringScheme,
} from '../types/competitionPracticalExecutionConfig';

export function getCompetitionPracticalExecutionConfig(stageId: CompetitionId) {
  return unwrap<CompetitionPracticalExecutionConfig | null>(
    apiClient.get(`/competition/stages/${stageId}/practical-execution-config`),
  );
}

export function listCompetitionPracticalScoringSchemes(scope: {
  eventId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
}) {
  return unwrap<CompetitionPracticalScoringScheme[]>(
    apiClient.get('/competition/practical-scoring-schemes', { params: scope }),
  );
}

export function validateCompetitionPracticalExecutionConfig(
  stageId: CompetitionId,
  request: CompetitionPracticalExecutionConfigRequest,
) {
  return unwrap<CompetitionPracticalExecutionConfigValidation>(
    apiClient.post(`/competition/stages/${stageId}/practical-execution-config/validate`, request),
  );
}

export function saveCompetitionPracticalExecutionConfig(
  stageId: CompetitionId,
  request: CompetitionPracticalExecutionConfigRequest,
) {
  return unwrap<CompetitionPracticalExecutionConfig>(
    apiClient.put(`/competition/stages/${stageId}/practical-execution-config`, request),
  );
}
