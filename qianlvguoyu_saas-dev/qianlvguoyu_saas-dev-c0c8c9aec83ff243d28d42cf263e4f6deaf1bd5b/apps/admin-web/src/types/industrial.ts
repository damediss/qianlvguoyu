export interface IndustrialCollege {
  id: string;
  name: string;
  tag?: string;
  businessScope?: string;
  description?: string;
  trainingQualification?: string;
  certificationQualification?: string;
  address?: string;
  phone?: string;
  status: 'enabled' | 'disabled';
  sortOrder: number;
  tenantId: number | string;
  createdAt?: string;
  updatedAt?: string;
}

export interface IndustrialCollegePayload {
  name: string;
  tag?: string;
  businessScope?: string;
  description?: string;
  trainingQualification?: string;
  certificationQualification?: string;
  address?: string;
  phone?: string;
  status?: 'enabled' | 'disabled';
  sortOrder?: number;
}

export interface IndustrialCollegeQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  tag?: string;
  status?: string;
}

export interface IndustrialCollegeStats {
  total: number;
  recentCreated: number;
  byTag: Array<{
    tag: string;
    count: number;
  }>;
}

export type IndustrialPlatformCategory = 'master' | 'study' | 'match' | 'apprentice' | 'comment';

export interface IndustrialPlatformItem {
  id: string;
  category: IndustrialPlatformCategory;
  company: string;
  name: string;
  nums?: number;
  date?: string;
  state?: string;
  tenantId: number | string;
  createdAt?: string;
  updatedAt?: string;
}

export interface IndustrialPlatformPayload {
  company: string;
  name: string;
  nums?: number;
  date?: string;
  state?: string;
}

export interface IndustrialPlatformQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  company?: string;
  state?: string;
}

export type ProjectResourceType = 'course' | 'teach' | 'exam' | 'train' | 'criterion';

export interface ProjectResource {
  id: string;
  type: ProjectResourceType;
  date: string;
  name: string;
  state: string;
  source?: string;
  company?: string;
  preview?: string;
  file_type?: 'pptx' | 'docx' | 'pdf';
  fileType?: 'pptx' | 'docx' | 'pdf';
}

export interface ProjectResourceQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  state?: string;
  source?: string;
  company?: string;
}

export interface IndustrialTalentTraining {
  id: string;
  name: string;
  level: string;
  workType?: string;
  work_type?: string;
  zhiwei: string;
  education: string;
}

export interface IndustrialTalentTrainingQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  workType?: string;
}

export interface IndustrialTalentService {
  id: string;
  title: string;
  source: string;
  date: string;
}

export interface IndustrialTalentServiceQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  source?: string;
}
