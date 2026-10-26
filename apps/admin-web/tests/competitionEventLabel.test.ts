import { describe, expect, it } from 'vitest';
import { competitionEventLabel } from '../src/utils/competitionEventLabel';

describe('competitionEventLabel', () => {
  it('先展示赛事名称，再展示赛事编码', () => {
    expect(competitionEventLabel({ name: '2026 年广西职业技能大赛', code: 'GX2026' }))
      .toBe('2026 年广西职业技能大赛 · GX2026');
  });

  it('名称或编码缺失时只展示存在的一项', () => {
    expect(competitionEventLabel({ name: '仅名称赛事', code: '' })).toBe('仅名称赛事');
    expect(competitionEventLabel({ name: '', code: 'ONLY_CODE' })).toBe('ONLY_CODE');
    expect(competitionEventLabel({ name: '  ', code: ' ' })).toBe('-');
  });
});
