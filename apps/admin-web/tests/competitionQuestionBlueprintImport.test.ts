import { describe, expect, it } from 'vitest';
import { getCompetitionQuestionBlueprintErrorMessage, hasLiveCompetitionQuestionBlueprint } from '../src/pages/competition/CompetitionQuestionBlueprintPage';

describe('竞赛实操细目表导入错误处理', () => {
  it('reads the server warning from an Axios response so confirmation can continue', () => {
    const warning = '细目表存在待确认警告，请确认后重新提交：节点 技能操作 XYZ 比例不一致';
    const error = {
      message: 'Request failed with status code 422',
      response: { status: 422, data: { code: '422', message: warning } },
    };

    expect(getCompetitionQuestionBlueprintErrorMessage(error)).toBe(warning);
  });

  it('falls back to the ordinary error message when no server message exists', () => {
    expect(getCompetitionQuestionBlueprintErrorMessage(new Error('文件解析失败'))).toBe('文件解析失败');
  });

  it('treats a draft or active row as the one live blueprint for a work type level', () => {
    expect(hasLiveCompetitionQuestionBlueprint([
      { status: 'archived' },
      { status: 'active' },
    ])).toBe(true);
    expect(hasLiveCompetitionQuestionBlueprint([{ status: 'draft' }])).toBe(true);
    expect(hasLiveCompetitionQuestionBlueprint([{ status: 'archived' }])).toBe(false);
    expect(hasLiveCompetitionQuestionBlueprint([])).toBe(false);
  });
});
