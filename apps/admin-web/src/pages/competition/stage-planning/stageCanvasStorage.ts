import type { StageCanvasNotes, StageEdge, StageNode } from '../../../types/competitionStagePlanning';
import { cleanStageNotes } from './stageGraphUtils';

const SCHEMA_VERSION = 1 as const;

export function stageCanvasStorageKey(
  tenantId: string | number,
  eventId: string | number,
  eventWorkTypeId?: string | number,
  eventWorkTypeLevelCode?: string,
) {
  if (eventWorkTypeId == null) {
    // Preserve the existing event-level key for historical canvas notes.
    return `guoyu:competition-stage-canvas:v${SCHEMA_VERSION}:${String(tenantId)}:${String(eventId)}`;
  }
  const levelSuffix = eventWorkTypeLevelCode == null
    ? ''
    : `:level:${String(eventWorkTypeLevelCode)}`;
  return `guoyu:competition-stage-canvas:v${SCHEMA_VERSION}:${String(tenantId)}:${String(eventId)}:work-type:${String(eventWorkTypeId)}${levelSuffix}`;
}

export function readStageCanvasNotes(
  tenantId: string | number,
  eventId: string | number,
  eventWorkTypeId?: string | number,
  eventWorkTypeLevelCode?: string,
): Record<string, string> {
  try {
    const raw = localStorage.getItem(stageCanvasStorageKey(
      tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode));
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Partial<StageCanvasNotes>;
    if (parsed.schemaVersion !== SCHEMA_VERSION
      || String(parsed.tenantId) !== String(tenantId)
      || String(parsed.eventId) !== String(eventId)
      || !parsed.notes || typeof parsed.notes !== 'object') {
      return {};
    }
    return Object.entries(parsed.notes).reduce<Record<string, string>>((result, [key, value]) => {
      if (typeof value === 'string') result[key] = value;
      return result;
    }, {});
  } catch {
    return {};
  }
}

export function writeStageCanvasNotes(
  tenantId: string | number,
  eventId: string | number,
  notes: Record<string, string>,
  eventWorkTypeId?: string | number,
  eventWorkTypeLevelCode?: string,
) {
  const value: StageCanvasNotes = {
    schemaVersion: SCHEMA_VERSION,
    tenantId: String(tenantId),
    eventId: String(eventId),
    notes,
  };
  localStorage.setItem(
    stageCanvasStorageKey(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode),
    JSON.stringify(value),
  );
}

export function cleanPersistedStageNotes(nodes: StageNode[], edges: StageEdge[], notes: Record<string, string>) {
  return cleanStageNotes(nodes, edges, notes);
}
