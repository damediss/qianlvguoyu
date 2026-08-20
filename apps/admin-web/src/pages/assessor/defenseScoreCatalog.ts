export const THESIS_REJECT_ITEMS = [
  '有严重抄袭行为或内容违背社会主义价值观。',
  '内容非本职业领域内容或不能体现本职业本级别技能特点。',
  '观点和方法严重错误。',
  '未按要求的题目或方向进行撰写',
];

export const THESIS_CONTENT_ITEMS = [
  { key: 'content_1', label: '论文内容的意义和难度', maxScore: 15 },
  { key: 'content_2', label: '论文内容的正确性', maxScore: 15 },
  { key: 'content_3', label: '论文结构的逻辑性', maxScore: 15 },
  { key: 'content_4', label: '论文的独创性及应用价值', maxScore: 15 },
  { key: 'content_5', label: '掌握基础理论知识的程度', maxScore: 15 },
  { key: 'content_6', label: '综合分析和解决问题的能力', maxScore: 15 },
  { key: 'content_7', label: '文字质量和书面表达能力', maxScore: 10 },
];

export const THESIS_DEFENSE_ITEMS = [
  { key: 'defense_1', label: '考生简述论文情况', maxScore: 25 },
  { key: 'defense_2', label: '回答问题正确性', maxScore: 25 },
  { key: 'defense_3', label: '对论文选题的理解程度', maxScore: 25 },
  { key: 'defense_4', label: '逻辑思维及口头表达能力', maxScore: 25 },
];

export interface PerformanceScoreItem {
  key: string;
  label: string;
  maxScore: number;
  criteria: string;
  disqualify?: boolean;
}

export interface PerformanceScoreCategory {
  key: string;
  label: string;
  maxScore: number;
  items: PerformanceScoreItem[];
}

export const PERFORMANCE_CATEGORIES: PerformanceScoreCategory[] = [
  {
    key: 'ability',
    label: '业务能力',
    maxScore: 30,
    items: [
      {
        key: 'duty_ability',
        label: '履行岗位职责能力',
        maxScore: 10,
        criteria: '1.承担重要技术工作，工作责任重大得9-10分；2.承担比较复杂的技术工作，解决较复杂技术问题得7-8分；3.承担一般性的技术工作，解决一般情况技术问题得5-6分。',
      },
      {
        key: 'work_performance',
        label: '工作业绩',
        maxScore: 20,
        criteria: '1.解决过国家级重大技术难题，或参与国家级科技项目酌情得16-20分。2.解决过省部级关键技术难题，或参与省部级科技项目酌情得14-18分。3.解决过地市级复杂技术难题，或参与地市级科技项目酌情得12-16分。4.解决过公司级一般技术难题，或参与公司级科技项目10-14分。',
      },
    ],
  },
  {
    key: 'guidance',
    label: '指导能力',
    maxScore: 30,
    items: [
      {
        key: 'papers_textbooks',
        label: '论文发表或编写教材',
        maxScore: 20,
        criteria: '在专业期刊发表论文的得10分，每增加一篇得5分；核心期刊以上的得15分，每增加一篇得5分。发表实用新型专利的得5分，每增加一篇得5分；发表发明专利的得10分，每增加一篇得10分。参与开发本职业技能国家职业标准、自治区题库编写、国家部委或自治区组织编写培训教材的酌情得1-5分。',
      },
      {
        key: 'learning',
        label: '学习能力',
        maxScore: 5,
        criteria: '1.3年内参加2次单位以外组织的业务培训得5分；2.3年内每次参加过单位以外的业务培训得4分（每次3分，最高5分）；3.3年内每次参加过单位组织的业务培训得3分（每次2分，最高5分）。',
      },
      {
        key: 'training_guidance',
        label: '培训指导',
        maxScore: 5,
        criteria: '在各种学习活动中承担授课、指导的酌情给分；或根据培养、传授徒弟的人数，徒弟技艺水平、比赛情况酌情给分。',
      },
    ],
  },
  {
    key: 'honor',
    label: '荣誉与奖励',
    maxScore: 20,
    items: [
      {
        key: 'awards',
        label: '奖励',
        maxScore: 10,
        criteria: '创造发明、技术革新、技术改造项目获得省、部级奖励得10分；市、厅级奖励得9分；获得县级奖励得8分。或参加省级一类竞赛前10名、二类竞赛前5名得10分；市级一类竞赛前10名、二类竞赛前5名得9分；县级一类竞赛前10名、二类或企业内部竞赛前5名得8分。获得荣誉证书省部级得10分，市厅级9分，县级8分，本单位获得荣誉按单位规格得分。',
      },
      {
        key: 'annual_review',
        label: '年度考核',
        maxScore: 10,
        criteria: '近3年的年度考核全部称职（合格）得6分，一次优秀加1分。',
      },
    ],
  },
  {
    key: 'qualification',
    label: '个人资历',
    maxScore: 20,
    items: [
      {
        key: 'education',
        label: '学历',
        maxScore: 5,
        criteria: '本科及以上学历5分，大专学历4分，中职及以下学历3分。',
      },
      {
        key: 'tenure',
        label: '任职年限',
        maxScore: 15,
        criteria: '任职资历满足申报条件得9分，每超过一年加1分。本项为否定项，经核实不符合申报条件的，综合科目成绩判定不合格，并按虚假申报取消所有科目成绩。',
        disqualify: true,
      },
    ],
  },
];

export function scoreOptions(maxScore: number) {
  return Array.from({ length: maxScore + 1 }, (_, score) => score);
}

export function sumScores(scores: Array<number | null | undefined> | undefined): number {
  return (scores || []).reduce<number>((total, score) => total + (typeof score === 'number' ? score : 0), 0);
}

export function roundScore(value: number) {
  return Math.round(value * 100) / 100;
}

export function thesisFinalScore(
  contentScores: Array<number | null | undefined> | undefined,
  defenseScores: Array<number | null | undefined> | undefined,
) {
  return roundScore(sumScores(contentScores) * 0.4 + sumScores(defenseScores) * 0.6);
}

export function performanceTotal(items?: Array<{ score?: number | null }> | null): number {
  return (items || []).reduce((sum, item) => sum + (item.score || 0), 0);
}

export function formatScore(value?: number | string | null) {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  const numeric = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(numeric)) {
    return '—';
  }
  return Number.isInteger(numeric) ? String(numeric) : numeric.toFixed(2);
}

export function statusLabel(status?: string) {
  if (status === 'submitted') return '已提交';
  if (status === 'draft') return '草稿';
  return '未评';
}
