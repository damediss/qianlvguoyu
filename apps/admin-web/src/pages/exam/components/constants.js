// 题库审核指派 - 管理端 3 页面共享常量
// 从 DevTaskDetailPage.tsx 抽出来,所有题型共用
// 级别映射:1高级技师 / 2技师 / 3高级工 / 4中级工 / 5初级工
export const LEVEL_OPTS = [
    { label: '初级工', value: 5 },
    { label: '中级工', value: 4 },
    { label: '高级工', value: 3 },
    { label: '技师', value: 2 },
    { label: '高级技师', value: 1 },
];
export const LEVEL_MAP = {
    1: '高级技师',
    2: '技师',
    3: '高级工',
    4: '中级工',
    5: '初级工',
};
// 审核状态映射:0待审核 1初审中 2终审中 3完成
export const STATUS_MAP = {
    0: { label: '待审核', color: 'default' },
    1: { label: '初审审核中', color: 'processing' },
    2: { label: '终审审核中', color: 'warning' },
    3: { label: '完成', color: 'success' },
};
// 角色映射:0初审 / 1终审
export const ROLE_MAP = {
    0: { label: '初审', color: 'green' },
    1: { label: '终审', color: 'red' },
};
// 题型中文
export const TASK_TYPE_CN = {
    exam: '理论题',
    examshicao: '实操题',
    examsub: '主观题',
    ximu: '细目表',
};
