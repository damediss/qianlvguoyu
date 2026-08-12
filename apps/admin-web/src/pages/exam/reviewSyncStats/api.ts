/**
 * AI 同步学统计接口封装 —— 《AI同步学统计功能设计方案》v1.1 §5.2
 * 管理端只读接口，权限码 exam:review-sync:stats:view / export
 */
import { apiClient, unwrap } from '../../../api/client';
import type { PageResponse } from '../../../types/api';

const BASE = '/exam/review-sync/stats';

// ------------------------------------------------------------------
// 类型
// ------------------------------------------------------------------

export interface StatsOverview {
  planCount: number;
  studentCount: number;
  todayActiveUsers: number;
  todayAnswers: number;
  totalAnswers: number;
  overallAccuracy: number;
  wrong: WrongDistribution;
}

export interface WrongDistribution {
  red: number;
  yellow: number;
  green: number;
  star: number;
  active: number;
}

export interface DailyTrendPoint {
  day: string;
  activeUsers: number;
  answers: number;
  accuracy: number;
  completionRate: number;
}

export interface WrongDailyPoint {
  day: string;
  cnt: number;
}

export interface WrongTrend {
  distribution: WrongDistribution;
  dailyNew: WrongDailyPoint[];
  dailyRemoved: WrongDailyPoint[];
}

export interface KnowledgeItem {
  knowledgeId: number | null;
  title: string;
  answers: number;
  accuracy: number;
  coverage?: number;
  answeredL3?: number;
  totalL3?: number;
}

export interface KnowledgeMastery {
  l1: KnowledgeItem[];
  l3: KnowledgeItem[];
}

export interface StudentRow {
  planId: string; // 雪花 id，字符串
  platformUserId: number;
  realname: string;
  phone: string | null;
  idcard: string | null;
  examId: number;
  examTitle: string;
  stage: number;
  dayIndex: number;
  totalDays: number;
  status: string;
  answers: number;
  newAnswers: number;
  reviewAnswers: number;
  mockAnswers: number;
  accuracy: number;
  redCnt: number;
  yellowCnt: number;
  greenCnt: number;
  starCnt: number;
  lastActiveDate: string | null;
}

export interface StudentDailyPoint {
  day: string;
  answers: number;
  accuracy: number;
}

export interface StudentWrongOption {
  content: string;
  correct: boolean;
}

export interface StudentWrongItem {
  questionId: number;
  questionType: string | null;
  content: string | null;
  options: StudentWrongOption[];
  correctAnswer: string | null;
  userAnswer: string | null;
  lastCorrect: boolean;
  cardLevel: string;
  star: number;
  removed: number;
  wrongCount: number;
  correctCount: number;
  lastWrongTime: string | null;
}

export interface StudentMockPaper {
  paperNo: number;
  score: number | null;
  totalScore: number | null;
  passScore: number | null;
  passed: boolean;
  correctCount: number | null;
  questionCount: number | null;
  createdAt: string | null;
}

export interface StudentDetail {
  plan: {
    planId: string;
    realname: string;
    phone: string | null;
    examId: number;
    examTitle: string;
    stage: number;
    dayIndex: number;
    totalDays: number;
    status: string;
    targetQuestions: number;
    startDate: string | null;
    examDate: string | null;
  };
  daily: StudentDailyPoint[];
  knowledge: KnowledgeMastery;
  wrongDistribution: WrongDistribution;
  wrongList: StudentWrongItem[];
  mockPapers: StudentMockPaper[];
}

export interface StatsQuery {
  from?: string;
  to?: string;
  examId?: number;
}

// ------------------------------------------------------------------
// API
// ------------------------------------------------------------------

export function fetchStatsOverview(q: StatsQuery) {
  return unwrap<StatsOverview>(apiClient.get(`${BASE}/overview`, { params: q }));
}

export function fetchDailyTrend(q: StatsQuery) {
  return unwrap<{ series: DailyTrendPoint[] }>(apiClient.get(`${BASE}/daily-trend`, { params: q }));
}

export function fetchWrongTrend(q: StatsQuery) {
  return unwrap<WrongTrend>(apiClient.get(`${BASE}/wrong-trend`, { params: q }));
}

export function fetchKnowledgeMastery(examId: number, platformUserId?: number) {
  return unwrap<KnowledgeMastery>(
    apiClient.get(`${BASE}/knowledge-mastery`, { params: { examId, platformUserId } }),
  );
}

export function fetchStudents(params: { page: number; pageSize: number; examId?: number; keyword?: string }) {
  return unwrap<PageResponse<StudentRow>>(apiClient.get(`${BASE}/students`, { params }));
}

export function fetchStudentDetail(planId: string) {
  return unwrap<StudentDetail>(apiClient.get(`${BASE}/students/${planId}`));
}

/** 学员明细 Excel 导出：返回 blob（含 5000 行上限的后端拦截） */
export async function exportStudents(params: { examId?: number; keyword?: string }): Promise<Blob> {
  const resp = await apiClient.get(`${BASE}/students/export`, {
    params,
    responseType: 'blob',
    timeout: 120000,
  });
  const blob = resp.data as Blob;
  // 业务异常（如超上限）以 JSON 形式返回，需转错误
  if (blob.type.includes('json')) {
    const text = await blob.text();
    try {
      const parsed = JSON.parse(text);
      throw new Error(parsed.message || '导出失败');
    } catch (e) {
      if (e instanceof SyntaxError) {
        throw new Error('导出失败');
      }
      throw e;
    }
  }
  return blob;
}

/** 触发浏览器下载 */
export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
