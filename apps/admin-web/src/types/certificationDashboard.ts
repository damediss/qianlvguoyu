export interface CertificationDashboardQuery {
  startDate?: string;
  endDate?: string;
  dateMode?: 'created' | 'exam' | 'archive';
  currentPhase?: string;
  status?: string;
  certificationType?: string;
  workTypeName?: string;
  levelName?: string;
}

export interface CertificationDashboardSummary {
  totalTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  taskCompletionRate: number;
  plannedPeopleCount: number;
  actualPeopleCount: number;
  overdueTasks: number;
  averageCycleDays: number;
  archivedTasks: number;
  pendingArchiveTasks: number;
  archiveCompletionRate: number;
  taskMaterialCompletionRate: number;
}

export interface CertificationDashboardQuality {
  openRiskCount: number;
  highRiskTaskCount: number;
  checklistCompletionRate: number;
  candidateMaterialCompletionRate: number;
  importErrorCount: number;
}

export interface CertificationDashboardFilterOptions {
  certificationTypes: string[];
  workTypes: string[];
  levels: string[];
}

export interface CertificationDashboardDistribution {
  code: string;
  name: string;
  taskCount: number;
  peopleCount: number;
}

export interface CertificationDashboardWorkTypeLevel {
  workTypeName: string;
  levelName: string;
  peopleCount: number;
}

export interface CertificationDashboardMonthlyTrend {
  month: string;
  createdTaskCount: number;
  archivedTaskCount: number;
  archivedPeopleCount: number;
}

export interface CertificationDashboardResource {
  code: string;
  name: string;
  total: number;
  available: number;
  busy: number;
  disabled: number;
  expiring: number;
  capacity: number;
}

export interface CertificationDashboardAlert {
  taskId?: string;
  taskNo?: string;
  taskName?: string;
  alertType: string;
  level: string;
  description: string;
  dueDate?: string;
}

export interface CertificationDashboard {
  summary: CertificationDashboardSummary;
  quality: CertificationDashboardQuality;
  filterOptions: CertificationDashboardFilterOptions;
  phaseDistribution: CertificationDashboardDistribution[];
  workTypeDistribution: CertificationDashboardDistribution[];
  levelDistribution: CertificationDashboardDistribution[];
  workTypeLevelDistribution: CertificationDashboardWorkTypeLevel[];
  monthlyTrends: CertificationDashboardMonthlyTrend[];
  resources: CertificationDashboardResource[];
  riskAlerts: CertificationDashboardAlert[];
  complianceAlerts: CertificationDashboardAlert[];
}
