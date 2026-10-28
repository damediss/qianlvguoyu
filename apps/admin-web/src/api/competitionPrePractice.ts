import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  CompetitionPrePracticeQuestionTypeCount,
  CompetitionPrePracticeDispatch,
  CompetitionPrePracticeDispatchCreateRequest,
  CompetitionPrePracticeDispatchQuery,
  CompetitionPrePracticePushContestant,
  CompetitionPrePracticePushOrganization,
  CompetitionPrePracticePushScopeQuery,
  CompetitionPrePracticeSourceScope,
  CompetitionPrePracticeTask,
  CompetitionPrePracticeTaskCreateRequest,
  CompetitionPrePracticeTaskQuery,
} from '../types/competitionPrePractice';

export function pageCompetitionPrePracticeTasks(params: CompetitionPrePracticeTaskQuery) {
  return unwrap<PageResponse<CompetitionPrePracticeTask>>(
    apiClient.get('/competition/pre-practice/tasks', { params }),
  );
}

export function getCompetitionPrePracticeQuestionCounts(sourceScope: CompetitionPrePracticeSourceScope) {
  return unwrap<CompetitionPrePracticeQuestionTypeCount[]>(
    apiClient.post('/competition/pre-practice/question-counts', { sourceScope }),
  );
}

export function createCompetitionPrePracticeTask(payload: CompetitionPrePracticeTaskCreateRequest) {
  return unwrap<CompetitionPrePracticeTask>(
    apiClient.post('/competition/pre-practice/tasks', payload),
  );
}

export function pageCompetitionPrePracticePushContestants(params: CompetitionPrePracticePushScopeQuery) {
  return unwrap<PageResponse<CompetitionPrePracticePushContestant>>(
    apiClient.get('/competition/pre-practice/push-contestants', { params }),
  );
}

export function listCompetitionPrePracticePushOrganizations(
  params: Omit<CompetitionPrePracticePushScopeQuery, 'page' | 'pageSize' | 'orgId'>,
) {
  return unwrap<CompetitionPrePracticePushOrganization[]>(
    apiClient.get('/competition/pre-practice/push-organizations', { params }),
  );
}

export function pageCompetitionPrePracticeAvailableTasks(
  params: Omit<CompetitionPrePracticePushScopeQuery, 'orgId'>,
) {
  return unwrap<PageResponse<CompetitionPrePracticeTask>>(
    apiClient.get('/competition/pre-practice/available-tasks', { params }),
  );
}

export function pageCompetitionPrePracticeDispatches(params: CompetitionPrePracticeDispatchQuery) {
  return unwrap<PageResponse<CompetitionPrePracticeDispatch>>(
    apiClient.get('/competition/pre-practice/dispatches', { params }),
  );
}

export function dispatchCompetitionPrePracticeTask(payload: CompetitionPrePracticeDispatchCreateRequest) {
  return unwrap<CompetitionPrePracticeDispatch>(
    apiClient.post('/competition/pre-practice/dispatches', payload),
  );
}

