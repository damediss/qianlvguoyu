// 来源映射:0=内部,1=外部,3=赛前复习
export const EXAM_SOURCE_MAP = {
    0: '内部',
    1: '外部',
    3: '赛前复习',
};
// 题型映射
export const EXAM_TYPE_MAP = {
    single: '单选题',
    multi: '多选题',
    judge: '判断题',
    fill: '填空题',
    short: '简答题',
    essay: '论述题',
    case: '案例分析',
    composite: '综合题',
    match: '匹配题',
    read: '阅读理解',
    practice: '实操题',
    drawing: '制图题',
    compute: '计算题',
    name: '名词解释',
    error_correction: '改错题',
    program: '编程题',
    oral: '口试题',
    drawing2: '画图题',
    cloze: '完形填空',
    translation: '翻译题',
    writing: '写作题',
};
// 难度映射
export const DIFFICULTY_MAP = {
    0: '易',
    1: '较易',
    2: '中',
    3: '较难',
    4: '难',
};
// 难度颜色
export const DIFFICULTY_COLOR_MAP = {
    0: 'green',
    1: 'blue',
    2: 'orange',
    3: 'red',
    4: '#8B0000',
};
// 审核状态映射
export const CHECK_STATUS_MAP = {
    0: '待审',
    1: '通过',
    2: '不通过',
};
