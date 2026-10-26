import type { CompetitionEvent } from '../types/competition';

/**
 * 赛事下拉选项统一文案（竞赛模块所有「选择赛事」下拉共用）。
 * 口径：先赛事名称、后赛事编码，例：2026 年广西职业技能大赛 · GX2026。
 */
export function competitionEventLabel(event: Pick<CompetitionEvent, 'name' | 'code'>): string {
  const name = event.name?.trim();
  const code = event.code?.trim();
  if (name && code) return `${name} · ${code}`;
  return name || code || '-';
}
