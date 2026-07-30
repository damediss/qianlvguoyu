import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  IndustrialCollege,
  IndustrialCollegePayload,
  IndustrialCollegeQuery,
  IndustrialCollegeStats,
  IndustrialPlatformCategory,
  IndustrialPlatformItem,
  IndustrialPlatformPayload,
  IndustrialPlatformQuery,
  IndustrialTalentService,
  IndustrialTalentServiceQuery,
  IndustrialTalentTraining,
  IndustrialTalentTrainingQuery,
  ProjectResource,
  ProjectResourceQuery,
  ProjectResourceType,
} from '../types/industrial';

export function pageIndustrialColleges(params: IndustrialCollegeQuery) {
  return unwrap<PageResponse<IndustrialCollege>>(apiClient.get('/industrial/college', { params }));
}

export function getIndustrialCollege(id: string) {
  return unwrap<IndustrialCollege>(apiClient.get(`/industrial/college/${id}`));
}

export function getIndustrialCollegeStats() {
  return unwrap<IndustrialCollegeStats>(apiClient.get('/industrial/college/stats'));
}

export function createIndustrialCollege(payload: IndustrialCollegePayload) {
  return unwrap<IndustrialCollege>(apiClient.post('/industrial/college', payload));
}

export function updateIndustrialCollege(id: string, payload: IndustrialCollegePayload) {
  return unwrap<IndustrialCollege>(apiClient.put(`/industrial/college/${id}`, payload));
}

export function deleteIndustrialCollege(id: string) {
  return unwrap<void>(apiClient.delete(`/industrial/college/${id}`));
}

const platformPath: Record<IndustrialPlatformCategory, string> = {
  master: 'platform-master',
  study: 'platform-study',
  match: 'platform-match',
  apprentice: 'platform-apprentice',
  comment: 'platform-comment',
};

export function pageIndustrialPlatformItems(category: IndustrialPlatformCategory, params: IndustrialPlatformQuery) {
  return unwrap<PageResponse<IndustrialPlatformItem>>(apiClient.get(`/industrial/${platformPath[category]}`, { params }));
}

export function createIndustrialPlatformItem(category: IndustrialPlatformCategory, payload: IndustrialPlatformPayload) {
  return unwrap<IndustrialPlatformItem>(apiClient.post(`/industrial/${platformPath[category]}`, payload));
}

export function updateIndustrialPlatformItem(
  category: IndustrialPlatformCategory,
  id: string,
  payload: IndustrialPlatformPayload,
) {
  return unwrap<IndustrialPlatformItem>(apiClient.put(`/industrial/${platformPath[category]}/${id}`, payload));
}

export function deleteIndustrialPlatformItem(category: IndustrialPlatformCategory, id: string) {
  return unwrap<void>(apiClient.delete(`/industrial/${platformPath[category]}/${id}`));
}

const projectResourcePath: Record<ProjectResourceType, string> = {
  course: 'projectcourse',
  teach: 'projectteach',
  exam: 'projectexam',
  train: 'projecttrain',
  criterion: 'projectcriterion',
};

export function pageProjectResources(type: ProjectResourceType, params: ProjectResourceQuery) {
  return unwrap<PageResponse<ProjectResource>>(apiClient.get(`/industrial/${projectResourcePath[type]}`, { params }));
}

export function pageTalentTraining(params: IndustrialTalentTrainingQuery) {
  return unwrap<PageResponse<IndustrialTalentTraining>>(apiClient.get('/industrial/talenttraining', { params }));
}

export function pageTalentService(params: IndustrialTalentServiceQuery) {
  return unwrap<PageResponse<IndustrialTalentService>>(apiClient.get('/industrial/talentservice', { params }));
}
