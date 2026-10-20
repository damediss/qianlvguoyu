import { cleanStageNotes } from './stageGraphUtils';
const SCHEMA_VERSION = 1;
export function stageCanvasStorageKey(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode) {
    if (eventWorkTypeId == null) {
        // Preserve the existing event-level key for historical canvas notes.
        return `guoyu:competition-stage-canvas:v${SCHEMA_VERSION}:${String(tenantId)}:${String(eventId)}`;
    }
    const levelSuffix = eventWorkTypeLevelCode == null
        ? ''
        : `:level:${String(eventWorkTypeLevelCode)}`;
    return `guoyu:competition-stage-canvas:v${SCHEMA_VERSION}:${String(tenantId)}:${String(eventId)}:work-type:${String(eventWorkTypeId)}${levelSuffix}`;
}
export function readStageCanvasNotes(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode) {
    try {
        const raw = localStorage.getItem(stageCanvasStorageKey(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode));
        if (!raw)
            return {};
        const parsed = JSON.parse(raw);
        if (parsed.schemaVersion !== SCHEMA_VERSION
            || String(parsed.tenantId) !== String(tenantId)
            || String(parsed.eventId) !== String(eventId)
            || !parsed.notes || typeof parsed.notes !== 'object') {
            return {};
        }
        return Object.entries(parsed.notes).reduce((result, [key, value]) => {
            if (typeof value === 'string')
                result[key] = value;
            return result;
        }, {});
    }
    catch {
        return {};
    }
}
export function writeStageCanvasNotes(tenantId, eventId, notes, eventWorkTypeId, eventWorkTypeLevelCode) {
    const value = {
        schemaVersion: SCHEMA_VERSION,
        tenantId: String(tenantId),
        eventId: String(eventId),
        notes,
    };
    localStorage.setItem(stageCanvasStorageKey(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode), JSON.stringify(value));
}
export function cleanPersistedStageNotes(nodes, edges, notes) {
    return cleanStageNotes(nodes, edges, notes);
}
